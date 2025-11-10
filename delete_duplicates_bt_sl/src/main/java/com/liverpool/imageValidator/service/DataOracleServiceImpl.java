package com.liverpool.imageValidator.service;

import com.liverpool.imageValidator.config.AppConfig;
import com.liverpool.imageValidator.models.SkusToDeleteDTO;
import com.liverpool.imageValidator.repository.InventoryRepository;
import com.liverpool.imageValidator.entity.Inventory;
import com.liverpool.imageValidator.repository.OracleDBRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataOracleServiceImpl implements DataOracleService {

    private final OracleDBRepository oracleDBRepository;
    private final AppConfig appConfig;
    private final InventoryRepository inventoryRepository;

    private int batchSize;

    @PostConstruct
    public void init() {
        this.batchSize = appConfig.getBatchSize();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getIuoSkus() {
        return oracleDBRepository.findIuoSkus();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getIuoSkusManual(List<String> skusArgs) {
        return oracleDBRepository.findIuoSkusManual(skusArgs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkusToDeleteDTO> getValidSkusToRemove(List<String> skusList) {
        if (skusList == null || skusList.isEmpty()) return Collections.emptyList();

        List<SkusToDeleteDTO> result = new ArrayList<>();
        for (int i = 0; i < skusList.size(); i += this.batchSize) {
            int end = Math.min(i + this.batchSize, skusList.size());
            List<String> batch = skusList.subList(i, end);
            List<SkusToDeleteDTO> partial = oracleDBRepository.findValidSkusByBatch(batch);
            if (partial != null && !partial.isEmpty()) {
                result.addAll(partial);
            }
        }
        return result;
    }

    @Transactional
    public long deleteSkusFromFileByType(String productType) throws IOException {
        Path dirDel = Paths.get("files", "deleteDB", "skuList").resolve(productType);
        Path file = productType.equals("BT")
                ? dirDel.resolve("BigTicket_List_Delete.txt")
                : dirDel.resolve("SoftLine_List_Delete.txt");

        if (!Files.exists(file)) {
            log.warn("File to delete not found: {}", file.toAbsolutePath());
            return 0L;
        }

        List<String> skus = Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        log.info("Total SKUs a eliminar ({}): {}", productType, skus.size());
        long totalDeleted = 0L;

        // 🧩 Collect async tasks
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < skus.size(); i += this.batchSize) {
            int end = Math.min(i + this.batchSize, skus.size());
            List<String> chunk = skus.subList(i, end);
            log.info("Eliminando chunk {}/{} size={}", (i / this.batchSize) + 1,
                    (int) Math.ceil((double) skus.size() / this.batchSize), chunk.size());

            int deleted = oracleDBRepository.deleteIuoBySkusInBatch(chunk, productType);
            totalDeleted += deleted;
            log.info("Filas eliminadas en chunk: {}", deleted);

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.error("Thread sleep interrupted", e);
                Thread.currentThread().interrupt();
            }

            // 🔄 Add async task to list
            futures.add(CompletableFuture.runAsync(() -> updateMongoAfterDeletion(chunk)));
        }

        // ✅ Wait for all async tasks before exiting
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("✅ All Mongo updates completed before exiting");

        log.info("Total filas eliminadas ({}): {}", productType, totalDeleted);
        return totalDeleted;
    }

    private void updateMongoAfterDeletion(List<String> chunk) {
        log.info("🚀 Mongo update thread started for chunk size={} (Thread: {})", chunk.size(), Thread.currentThread().getName());

        for (String sku : chunk) {
            Optional<Inventory> optionalInventory = inventoryRepository.findById(sku);

            if (!optionalInventory.isPresent()) {
                log.warn("⚠️ SKU {} not found in Mongo. Skipping.", sku);
                continue;
            }

            Inventory inventoryDoc = optionalInventory.get();
            List<Object> inventoryList = inventoryDoc.getInventory();

            if (inventoryList == null || inventoryList.isEmpty()) {
                // Case 2
                inventoryList = new ArrayList<>();
                Map<String, String> onlineStore = new HashMap<>();
                onlineStore.put("storeId", "online");
                inventoryList.add(onlineStore);
                inventoryDoc.setInventory(inventoryList);
                inventoryRepository.save(inventoryDoc);
                log.info("Initialized empty inventory list and added online storeId for SKU {} (Case 2)", sku);
            } else {
                boolean hasOnline = inventoryList.stream()
                        .filter(item -> item instanceof Map)
                        .anyMatch(item -> "online".equals(((Map<?, ?>) item).get("storeId")));
                if (hasOnline) {
                    // Clean only the online entry, removing extra fields but keep others unchanged
                    List<Object> cleanedList = new ArrayList<>();
                    for (Object item : inventoryList) {
                        if (item instanceof Map) {
                            Map<?, ?> mapItem = (Map<?, ?>) item;
                            Object storeId = mapItem.get("storeId");
                            if ("online".equals(storeId)) {
                                Map<String, String> cleanedOnline = new HashMap<>();
                                cleanedOnline.put("storeId", "online");
                                cleanedList.add(cleanedOnline);
                            } else {
                                cleanedList.add(item);
                            }
                        } else {
                            cleanedList.add(item);
                        }
                    }
                    inventoryDoc.setInventory(cleanedList);
                    inventoryRepository.save(inventoryDoc);
                    log.info("Cleaned extra fields from online storeId for SKU {} (Case 4 updated)", sku);
                } else {
                    Map<String, String> onlineStore = new HashMap<>();
                    onlineStore.put("storeId", "online");
                    inventoryList.add(onlineStore);
                    inventoryDoc.setInventory(inventoryList);
                    inventoryRepository.save(inventoryDoc);
                    log.info("➕ Appended online storeId to inventory list for SKU {} (Case 3)", sku);
                }
            }
        }
    }

}
