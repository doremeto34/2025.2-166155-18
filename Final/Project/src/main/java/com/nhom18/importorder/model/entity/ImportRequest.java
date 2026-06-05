package com.nhom18.importorder.model.entity;

import com.nhom18.importorder.model.enums.RequestStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ImportRequest {
    private int id;
    private int createdBy;
    private LocalDate createdDate;
    private RequestStatus status;
    private List<ImportRequestItem> items = new ArrayList<>();
    
    // Helper field for UI display (joined data)
    private String creatorName;

    public ImportRequest() {}

    public ImportRequest(int id, int createdBy, LocalDate createdDate, RequestStatus status) {
        this.id = id;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public List<ImportRequestItem> getItems() {
        return items;
    }

    public void setItems(List<ImportRequestItem> items) {
        this.items = items;
    }

    public void addItem(ImportRequestItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }
}
