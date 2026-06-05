package com.nhom18.importorder.model.entity;

public class Merchandise {
    private String merchandiseCode;
    private String name;
    private String description;
    private String unit;
    private double price;
    private boolean active;

    public Merchandise() {}

    public Merchandise(String merchandiseCode, String name, String description, String unit, double price, boolean active) {
        this.merchandiseCode = merchandiseCode;
        this.name = name;
        this.description = description;
        this.unit = unit;
        this.price = price;
        this.active = active;
    }

    public String getMerchandiseCode() {
        return merchandiseCode;
    }

    public void setMerchandiseCode(String merchandiseCode) {
        this.merchandiseCode = merchandiseCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return name + " (" + merchandiseCode + ")";
    }
}
