package com.nhom18.importorder.model.entity;

import java.time.LocalDate;

public class ImportRequestItem {
    private int id;
    private int requestId;
    private String merchandiseCode;
    private int quantityOrdered;
    private int quantityShortage;
    private String unit;
    private LocalDate desiredDeliveryDate;
    
    // Helper field for UI display (joined data)
    private String merchandiseName;

    public ImportRequestItem() {}

    public ImportRequestItem(int id, int requestId, String merchandiseCode, int quantityOrdered, String unit, LocalDate desiredDeliveryDate) {
        this.id = id;
        this.requestId = requestId;
        this.merchandiseCode = merchandiseCode;
        this.quantityOrdered = quantityOrdered;
        this.quantityShortage = quantityOrdered; // default to ordered quantity if not specified
        this.unit = unit;
        this.desiredDeliveryDate = desiredDeliveryDate;
    }

    public ImportRequestItem(int id, int requestId, String merchandiseCode, int quantityOrdered, int quantityShortage, String unit, LocalDate desiredDeliveryDate) {
        this.id = id;
        this.requestId = requestId;
        this.merchandiseCode = merchandiseCode;
        this.quantityOrdered = quantityOrdered;
        this.quantityShortage = quantityShortage;
        this.unit = unit;
        this.desiredDeliveryDate = desiredDeliveryDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDate getDesiredDeliveryDate() {
        return desiredDeliveryDate;
    }

    public void setDesiredDeliveryDate(LocalDate desiredDeliveryDate) {
        this.desiredDeliveryDate = desiredDeliveryDate;
    }

    public String getMerchandiseName() {
        return merchandiseName;
    }

    public void setMerchandiseName(String merchandiseName) {
        this.merchandiseName = merchandiseName;
    }

    public int getQuantityShortage() {
        return quantityShortage;
    }

    public void setQuantityShortage(int quantityShortage) {
        this.quantityShortage = quantityShortage;
    }
}
