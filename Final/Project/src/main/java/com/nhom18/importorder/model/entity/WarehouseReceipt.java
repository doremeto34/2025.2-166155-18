package com.nhom18.importorder.model.entity;

import java.time.LocalDate;

public class WarehouseReceipt {
    private int id;
    private int orderId;
    private int confirmedBy;
    private LocalDate confirmDate;
    private String notes;

    // Helper field for UI
    private String confirmedByName;

    public WarehouseReceipt() {}

    public WarehouseReceipt(int id, int orderId, int confirmedBy, LocalDate confirmDate, String notes) {
        this.id = id;
        this.orderId = orderId;
        this.confirmedBy = confirmedBy;
        this.confirmDate = confirmDate;
        this.notes = notes;
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

    public int getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(int confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public LocalDate getConfirmDate() {
        return confirmDate;
    }

    public void setConfirmDate(LocalDate confirmDate) {
        this.confirmDate = confirmDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getConfirmedByName() {
        return confirmedByName;
    }

    public void setConfirmedByName(String confirmedByName) {
        this.confirmedByName = confirmedByName;
    }
}
