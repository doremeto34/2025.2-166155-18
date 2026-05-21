package com.nhom18.importorder.model.entity;

import com.nhom18.importorder.model.enums.DeliveryMethod;
import com.nhom18.importorder.model.enums.OrderStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private int id;
    private int requestId;
    private String siteCode;
    private DeliveryMethod deliveryMethod;
    private OrderStatus status;
    private LocalDate createdDate;
    private LocalDate estimatedArrival;
    private String cancelReason;
    private List<OrderItem> items = new ArrayList<>();

    // Thuộc tính phụ hiển thị thông tin ở UI
    private String siteName;

    public Order() {}

    public Order(int id, int requestId, String siteCode, DeliveryMethod deliveryMethod, OrderStatus status, LocalDate createdDate, LocalDate estimatedArrival, String cancelReason) {
        this.id = id;
        this.requestId = requestId;
        this.siteCode = siteCode;
        this.deliveryMethod = deliveryMethod;
        this.status = status;
        this.createdDate = createdDate;
        this.estimatedArrival = estimatedArrival;
        this.cancelReason = cancelReason;
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

    public String getSiteCode() {
        return siteCode;
    }

    public void setSiteCode(String siteCode) {
        this.siteCode = siteCode;
    }

    public DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDate getEstimatedArrival() {
        return estimatedArrival;
    }

    public void setEstimatedArrival(LocalDate estimatedArrival) {
        this.estimatedArrival = estimatedArrival;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public void addItem(OrderItem item) {
        this.items.add(item);
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }
}
