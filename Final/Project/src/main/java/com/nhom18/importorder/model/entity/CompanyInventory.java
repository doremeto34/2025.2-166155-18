package com.nhom18.importorder.model.entity;

public class CompanyInventory {
    private String merchandiseCode;
    private int inStockQuantity;
    private String unit;
    
    // Auxiliary field for UI
    private String merchandiseName;

    public CompanyInventory() {}

    public CompanyInventory(String merchandiseCode, int inStockQuantity, String unit) {
        this.merchandiseCode = merchandiseCode;
        this.inStockQuantity = inStockQuantity;
        this.unit = unit;
    }

    public String getMerchandiseCode() {
        return merchandiseCode;
    }

    public void setMerchandiseCode(String merchandiseCode) {
        this.merchandiseCode = merchandiseCode;
    }

    public int getInStockQuantity() {
        return inStockQuantity;
    }

    public void setInStockQuantity(int inStockQuantity) {
        this.inStockQuantity = inStockQuantity;
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
