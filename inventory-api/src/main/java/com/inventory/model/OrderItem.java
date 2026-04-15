package com.inventory.model;

/**
 * Snapshot of a product at the time of ordering.
 * Prices stored as Double (same as Product.price) for consistency and sortability.
 */
public class OrderItem {

    private String productId;
    private String productName;
    private int quantity;
    private Double unitPrice;
    private Double subtotal;

    public OrderItem() {}

    public OrderItem(String productId, String productName, int quantity, Double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = unitPrice * quantity;
    }

    public String getProductId()                   { return productId; }
    public void setProductId(String productId)     { this.productId = productId; }
    public String getProductName()                 { return productName; }
    public void setProductName(String n)           { this.productName = n; }
    public int getQuantity()                       { return quantity; }
    public void setQuantity(int quantity)          { this.quantity = quantity; }
    public Double getUnitPrice()                   { return unitPrice; }
    public void setUnitPrice(Double p)             { this.unitPrice = p; }
    public Double getSubtotal()                    { return subtotal; }
    public void setSubtotal(Double s)              { this.subtotal = s; }
}
