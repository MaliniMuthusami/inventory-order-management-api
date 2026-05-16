package com.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class OrderRequest {

    public static class PlaceOrder {
        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        private List<OrderItemRequest> items;

        public List<OrderItemRequest> getItems()       { return items; }
        public void setItems(List<OrderItemRequest> i) { this.items = i; }
    }

    public static class OrderItemRequest {
        @NotBlank(message = "Product ID is required")
        private String productId;

        @NotNull
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        public String getProductId()                   { return productId; }
        public void setProductId(String productId)     { this.productId = productId; }
        public Integer getQuantity()                   { return quantity; }
        public void setQuantity(Integer quantity)      { this.quantity = quantity; }
    }
}
