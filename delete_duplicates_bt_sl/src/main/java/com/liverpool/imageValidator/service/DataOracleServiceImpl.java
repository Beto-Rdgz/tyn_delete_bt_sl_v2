package com.liverpool.imageValidator.service;

import com.liverpool.imageValidator.config.AppConfig;
import com.liverpool.imageValidator.models.SkusToDeleteDTO;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class DataOracleServiceImpl implements DataOracleService {

    private final OracleDBRepository oracleDBRepository;
    private final AppConfig appConfig;

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

        for (int i = 0; i < skus.size(); i += this.batchSize) {
            int end = Math.min(i + this.batchSize, skus.size());
            List<String> chunk = skus.subList(i, end);
            log.info("Eliminando chunk {}/{} size={}", (i / this.batchSize) + 1,
                    (int) Math.ceil((double) skus.size() / this.batchSize), chunk.size());

            int deleted = oracleDBRepository.deleteIuoBySkusInBatch(chunk, productType);
            totalDeleted += deleted;
            log.info("Filas eliminadas en chunk: {}", deleted);
        }

        log.info("Total filas eliminadas ({}): {}", productType, totalDeleted);
        return totalDeleted;
    }

}
