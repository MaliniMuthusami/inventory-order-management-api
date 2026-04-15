package com.inventory.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "products")
public class Product {

    @Id
    private String id;

    private String name;
    private String description;
    /**
     * Stored as Double so MongoDB can sort natively.
     * BigDecimal (Decimal128) sorting is unreliable with Flapdoodle embedded Mongo.
     * DTOs convert to/from BigDecimal for API presentation.
     */
    private Double price;
    private int stockQuantity;

    /** IDs of categories this product belongs to (Many-to-Many via reference list). */
    private List<String> categoryIds = new ArrayList<>();

    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {}

    public Product(String name, String description, Double price, int stockQuantity) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId()                              { return id; }
    public void setId(String id)                       { this.id = id; }
    public String getName()                            { return name; }
    public void setName(String name)                   { this.name = name; }
    public String getDescription()                     { return description; }
    public void setDescription(String d)               { this.description = d; }
    public Double getPrice()                           { return price; }
    public void setPrice(Double price)                 { this.price = price; }
    public int getStockQuantity()                      { return stockQuantity; }
    public void setStockQuantity(int qty)              { this.stockQuantity = qty; }
    public List<String> getCategoryIds()               { return categoryIds; }
    public void setCategoryIds(List<String> ids)       { this.categoryIds = ids; }
    public boolean isActive()                          { return active; }
    public void setActive(boolean active)              { this.active = active; }
    public LocalDateTime getCreatedAt()                { return createdAt; }
    public void setCreatedAt(LocalDateTime t)          { this.createdAt = t; }
    public LocalDateTime getUpdatedAt()                { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)          { this.updatedAt = t; }
}
