package com.inventory.dto.response;

import com.inventory.model.OrderStatus;
import com.inventory.model.Role;

import java.time.LocalDateTime;
import java.util.List;

public class ApiResponse {

    // ── Auth ──────────────────────────────────────────────────────────────────

    public static class Auth {
        private String token;
        private String type;
        private String username;
        private String email;
        private Role role;

        public Auth() {}
        public Auth(String token, String type, String username, String email, Role role) {
            this.token = token; this.type = type;
            this.username = username; this.email = email; this.role = role;
        }

        public String getToken()               { return token; }
        public void setToken(String t)         { this.token = t; }
        public String getType()                { return type; }
        public void setType(String t)          { this.type = t; }
        public String getUsername()            { return username; }
        public void setUsername(String u)      { this.username = u; }
        public String getEmail()               { return email; }
        public void setEmail(String e)         { this.email = e; }
        public Role getRole()                  { return role; }
        public void setRole(Role r)            { this.role = r; }
    }

    // ── Category ──────────────────────────────────────────────────────────────

    public static class CategoryResponse {
        private String id;
        private String name;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public CategoryResponse() {}

        public String getId()                          { return id; }
        public void setId(String id)                   { this.id = id; }
        public String getName()                        { return name; }
        public void setName(String name)               { this.name = name; }
        public String getDescription()                 { return description; }
        public void setDescription(String d)           { this.description = d; }
        public LocalDateTime getCreatedAt()            { return createdAt; }
        public void setCreatedAt(LocalDateTime t)      { this.createdAt = t; }
        public LocalDateTime getUpdatedAt()            { return updatedAt; }
        public void setUpdatedAt(LocalDateTime t)      { this.updatedAt = t; }
    }

    // ── Product ───────────────────────────────────────────────────────────────

    public static class ProductResponse {
        private String id;
        private String name;
        private String description;
        private Double price;
        private int stockQuantity;
        private List<CategoryResponse> categories;
        private boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ProductResponse() {}

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
        public List<CategoryResponse> getCategories()      { return categories; }
        public void setCategories(List<CategoryResponse> c){ this.categories = c; }
        public boolean isActive()                          { return active; }
        public void setActive(boolean active)              { this.active = active; }
        public LocalDateTime getCreatedAt()                { return createdAt; }
        public void setCreatedAt(LocalDateTime t)          { this.createdAt = t; }
        public LocalDateTime getUpdatedAt()                { return updatedAt; }
        public void setUpdatedAt(LocalDateTime t)          { this.updatedAt = t; }
    }

    // ── Order ─────────────────────────────────────────────────────────────────

    public static class OrderItemResponse {
        private String productId;
        private String productName;
        private int quantity;
        private Double unitPrice;
        private Double subtotal;

        public OrderItemResponse() {}

        public String getProductId()                   { return productId; }
        public void setProductId(String id)            { this.productId = id; }
        public String getProductName()                 { return productName; }
        public void setProductName(String n)           { this.productName = n; }
        public int getQuantity()                       { return quantity; }
        public void setQuantity(int q)                 { this.quantity = q; }
        public Double getUnitPrice()                   { return unitPrice; }
        public void setUnitPrice(Double p)             { this.unitPrice = p; }
        public Double getSubtotal()                    { return subtotal; }
        public void setSubtotal(Double s)              { this.subtotal = s; }
    }

    public static class OrderResponse {
        private String id;
        private String userId;
        private String username;
        private List<OrderItemResponse> items;
        private Double totalAmount;
        private OrderStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public OrderResponse() {}

        public String getId()                              { return id; }
        public void setId(String id)                       { this.id = id; }
        public String getUserId()                          { return userId; }
        public void setUserId(String u)                    { this.userId = u; }
        public String getUsername()                        { return username; }
        public void setUsername(String u)                  { this.username = u; }
        public List<OrderItemResponse> getItems()          { return items; }
        public void setItems(List<OrderItemResponse> i)    { this.items = i; }
        public Double getTotalAmount()                     { return totalAmount; }
        public void setTotalAmount(Double t)               { this.totalAmount = t; }
        public OrderStatus getStatus()                     { return status; }
        public void setStatus(OrderStatus s)               { this.status = s; }
        public LocalDateTime getCreatedAt()                { return createdAt; }
        public void setCreatedAt(LocalDateTime t)          { this.createdAt = t; }
        public LocalDateTime getUpdatedAt()                { return updatedAt; }
        public void setUpdatedAt(LocalDateTime t)          { this.updatedAt = t; }
    }

    // ── Generic ───────────────────────────────────────────────────────────────

    public static class MessageResponse {
        private String message;
        private boolean success;

        public MessageResponse() {}
        public MessageResponse(String message, boolean success) {
            this.message = message; this.success = success;
        }

        public static MessageResponse ok(String message)   { return new MessageResponse(message, true); }
        public static MessageResponse fail(String message) { return new MessageResponse(message, false); }

        public String getMessage()              { return message; }
        public void setMessage(String m)        { this.message = m; }
        public boolean isSuccess()              { return success; }
        public void setSuccess(boolean s)       { this.success = s; }
    }

    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private LocalDateTime timestamp;

        public ErrorResponse() {}
        public ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
            this.status = status; this.error = error;
            this.message = message; this.timestamp = timestamp;
        }

        public int getStatus()                         { return status; }
        public void setStatus(int s)                   { this.status = s; }
        public String getError()                       { return error; }
        public void setError(String e)                 { this.error = e; }
        public String getMessage()                     { return message; }
        public void setMessage(String m)               { this.message = m; }
        public LocalDateTime getTimestamp()            { return timestamp; }
        public void setTimestamp(LocalDateTime t)      { this.timestamp = t; }
    }
}
