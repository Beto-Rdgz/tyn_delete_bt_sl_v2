package com.liverpool.imageValidator.service;

import com.liverpool.imageValidator.entity.Inventory;
import com.liverpool.imageValidator.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class InventoryCleanupService {

    @Autowired
    private InventoryRepository inventoryRepository;

    public void cleanInventoryFields(List<String> skusSoftLine) {
        for (String sku : skusSoftLine) {
            try {
                Optional<Inventory> optionalInventory = inventoryRepository.findById(sku);
                if (optionalInventory.isPresent()) {
                    Inventory inventoryDoc = optionalInventory.get();
                    List<Object> originalInventory = inventoryDoc.getInventory();
                    List<Object> updatedInventory = new ArrayList<>();

                    for (Object obj : originalInventory) {
                        if (obj instanceof Map) {
                            Map<String, Object> inv = (Map<String, Object>) obj;

                            // ✅ Only clean entries where storeId == "online"
                            Object storeId = inv.get("storeId");
                            if ("online".equalsIgnoreCase(String.valueOf(storeId))) {
                                Map<String, Object> cleaned = new HashMap<>();
                                cleaned.put("storeId", storeId);
                                if (inv.containsKey("stock")) {
                                    cleaned.put("stock", inv.get("stock"));
                                }
                                updatedInventory.add(cleaned);
                            } else {
                                // keep all fields as is for physical stores
                                updatedInventory.add(inv);
                            }
                        }
                    }

                    inventoryDoc.setInventory(updatedInventory);

                    // 🧾 Log before saving the document
                    log.info("💾 About to save cleaned inventory document for SKU {}: {}", sku, inventoryDoc);

                    inventoryRepository.save(inventoryDoc);

                    log.info("✅ Cleaned SKU (only 'online' entries): {}", sku);
                } else {
                    log.info("⚠️ SKU not found: {}", sku);
                }
            } catch (Exception e) {
                log.error("❌ Error cleaning SKU {}: {}", sku, e.getMessage(), e);
            }
        }
    }
}