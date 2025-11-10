package com.liverpool.imageValidator.repository;

import com.liverpool.imageValidator.entity.Inventory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories
public interface InventoryRepository extends MongoRepository<Inventory, String> {



}
