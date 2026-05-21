package com.nhom18.importorder.model.entity;

public class OrderItem {
    private int id;
    private int orderId;
    private String merchandiseCode;
    private int quantityOrdered;
    private int quantityConfirmed;
    private int quantityReceived;
    private String unit;

    // Thuộc tính phụ hiển thị trên UI
    private String merchandiseName;

    public OrderItem() {}

    public OrderItem(int id, int orderId, String merchandiseCode, int quantityOrdered, int quantityConfirmed, int quantityReceived, String unit) {
        this.id = id;
        this.orderId = orderId;
        this.merchandiseCode = merchandiseCode;
        this.quantityOrdered = quantityOrdered;
        this.quantityConfirmed = quantityConfirmed;
        this.quantityReceived = quantityReceived;
        this.unit = unit;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getMerchandiseCode() {
        return merchandiseCode;
    }

    public void setMerchandiseCode(String merchandiseCode) {
        this.merchandiseCode = merchandiseCode;
    }

    public int getQuantityOrdered() {
        return quantityOrdered;
    }

    public void setQuantityOrdered(int quantityOrdered) {
        this.quantityOrdered = quantityOrdered;
    }

    public int getQuantityConfirmed() {
        return quantityConfirmed;
    }

    public void setQuantityConfirmed(int quantityConfirmed) {
        this.quantityConfirmed = quantityConfirmed;
    }

    public int getQuantityReceived() {
        return quantityReceived;
    }

    public void setQuantityReceived(int quantityReceived) {
        this.quantityReceived = quantityReceived;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getMerchandiseName() {
        return merchandiseName;
    }

    public void setMerchandiseName(String merchandiseName) {
        this.merchandiseName = merchandiseName;
    }
}
