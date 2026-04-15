package com.inventory.dto.request;

import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

public class ProductRequest {

    public static class Create {
        @NotBlank(message = "Product name is required")
        @Size(max = 200)
        private String name;

        @Size(max = 1000)
        private String description;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        private Double price;

        @Min(value = 0, message = "Stock quantity cannot be negative")
        private int stockQuantity;

        private List<String> categoryIds = new ArrayList<>();

        public String getName()                        { return name; }
        public void setName(String name)               { this.name = name; }
        public String getDescription()                 { return description; }
        public void setDescription(String d)           { this.description = d; }
        public Double getPrice()                       { return price; }
        public void setPrice(Double price)             { this.price = price; }
        public int getStockQuantity()                  { return stockQuantity; }
        public void setStockQuantity(int qty)          { this.stockQuantity = qty; }
        public List<String> getCategoryIds()           { return categoryIds; }
        public void setCategoryIds(List<String> ids)   { this.categoryIds = ids; }
    }

    public static class Update {
        @Size(max = 200)
        private String name;

        @Size(max = 1000)
        private String description;

        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        private Double price;

        @Min(value = 0)
        private Integer stockQuantity;

        private List<String> categoryIds;
        private Boolean active;

        public String getName()                        { return name; }
        public void setName(String name)               { this.name = name; }
        public String getDescription()                 { return description; }
        public void setDescription(String d)           { this.description = d; }
        public Double getPrice()                       { return price; }
        public void setPrice(Double price)             { this.price = price; }
        public Integer getStockQuantity()              { return stockQuantity; }
        public void setStockQuantity(Integer qty)      { this.stockQuantity = qty; }
        public List<String> getCategoryIds()           { return categoryIds; }
        public void setCategoryIds(List<String> ids)   { this.categoryIds = ids; }
        public Boolean getActive()                     { return active; }
        public void setActive(Boolean active)          { this.active = active; }
    }
}
