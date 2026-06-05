package com.nhom18.importorder.controller.bpqlk;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class ReconciliationRow {
    private final int itemId;
    private final String merchCode;
    private final String merchName;
    private final int qtyOrdered;
    private final int qtyConfirmed;
    private final String unit;
    
    private final SimpleBooleanProperty checked = new SimpleBooleanProperty(false);
    private final SimpleIntegerProperty qtyReceived = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty discrepancy = new SimpleIntegerProperty(0);
    private final SimpleStringProperty notes = new SimpleStringProperty("");

    public ReconciliationRow(int itemId, String merchCode, String merchName, int qtyOrdered, int qtyConfirmed, String unit) {
        this.itemId = itemId;
        this.merchCode = merchCode;
        this.merchName = merchName;
        this.qtyOrdered = qtyOrdered;
        this.qtyConfirmed = qtyConfirmed;
        this.unit = unit;
        
        // Mặc định gán số thực nhận bằng số site xác nhận giao (để giảm công nhập liệu)
        setQtyReceived(qtyConfirmed);
    }

    public int getItemId() { return itemId; }
    public String getMerchCode() { return merchCode; }
    public String getMerchName() { return merchName; }
    public int getQtyOrdered() { return qtyOrdered; }
    public int getQtyConfirmed() { return qtyConfirmed; }
    public String getUnit() { return unit; }

    public SimpleBooleanProperty checkedProperty() { return checked; }
    public boolean isChecked() { return checked.get(); }
    public void setChecked(boolean checked) { this.checked.set(checked); }

    public SimpleIntegerProperty qtyReceivedProperty() { return qtyReceived; }
    public int getQtyReceived() { return qtyReceived.get(); }
    public void setQtyReceived(int qtyReceived) { 
        this.qtyReceived.set(qtyReceived);
        this.discrepancy.set(qtyReceived - qtyConfirmed);
    }

    public SimpleIntegerProperty discrepancyProperty() { return discrepancy; }
    public int getDiscrepancy() { return discrepancy.get(); }

    public SimpleStringProperty notesProperty() { return notes; }
    public String getNotes() { return notes.get(); }
    public void setNotes(String notes) { this.notes.set(notes); }
}
