package com.inventory.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private String userId;
    private String username;

    private List<OrderItem> items = new ArrayList<>();
    private Double totalAmount;
    private OrderStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Order() {}

    public Order(String userId, String username, List<OrderItem> items) {
        this.userId = userId;
        this.username = username;
        this.items = items;
        this.totalAmount = items.stream()
                .mapToDouble(OrderItem::getSubtotal)
                .sum();
        this.status = OrderStatus.CREATED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId()                          { return id; }
    public void setId(String id)                   { this.id = id; }
    public String getUserId()                      { return userId; }
    public void setUserId(String userId)           { this.userId = userId; }
    public String getUsername()                    { return username; }
    public void setUsername(String username)       { this.username = username; }
    public List<OrderItem> getItems()              { return items; }
    public void setItems(List<OrderItem> items)    { this.items = items; }
    public Double getTotalAmount()                 { return totalAmount; }
    public void setTotalAmount(Double t)           { this.totalAmount = t; }
    public OrderStatus getStatus()                 { return status; }
    public void setStatus(OrderStatus status)      { this.status = status; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
    public void setCreatedAt(LocalDateTime t)      { this.createdAt = t; }
    public LocalDateTime getUpdatedAt()            { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)      { this.updatedAt = t; }
}
