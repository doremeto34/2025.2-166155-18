package com.nhom18.importorder.service;

import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.model.enums.DeliveryMethod;
import java.time.LocalDate;

public class AllocationDetail {
    private final Site site;
    private final DeliveryMethod method;
    private int allocatedQuantity;
    private final LocalDate estimatedArrivalDate;

    public AllocationDetail(Site site, DeliveryMethod method, int allocatedQuantity, LocalDate estimatedArrivalDate) {
        this.site = site;
        this.method = method;
        this.allocatedQuantity = allocatedQuantity;
        this.estimatedArrivalDate = estimatedArrivalDate;
    }

    public Site getSite() { return site; }
    public DeliveryMethod getMethod() { return method; }
    public int getAllocatedQuantity() { return allocatedQuantity; }
    public void setAllocatedQuantity(int qty) { this.allocatedQuantity = qty; }
    public LocalDate getEstimatedArrivalDate() { return estimatedArrivalDate; }
}
