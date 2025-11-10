package com.liverpool.imageValidator.entity;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "inventory")
public class Inventory {

    @Id
    @JsonIgnore
    private String _id;
    private Boolean hasOnlineInventory;
    private List<Object> inventory;
    private Boolean hasOnlineInventory_Previous;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}