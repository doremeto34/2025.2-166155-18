package com.nhom18.importorder.model.entity;

public class SiteInventory {
    private String siteCode;
    private String merchandiseCode;
    private int inStockQuantity;
    private String unit;

    public SiteInventory() {}

    public SiteInventory(String siteCode, String merchandiseCode, int inStockQuantity, String unit) {
        this.siteCode = siteCode;
        this.merchandiseCode = merchandiseCode;
        this.inStockQuantity = inStockQuantity;
        this.unit = unit;
    }

    public String getSiteCode() {
        return siteCode;
    }

    public void setSiteCode(String siteCode) {
        this.siteCode = siteCode;
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

    private String merchandiseName;

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
