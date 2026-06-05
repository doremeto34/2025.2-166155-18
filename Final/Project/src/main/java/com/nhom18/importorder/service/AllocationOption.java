package com.nhom18.importorder.service;

import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.model.enums.DeliveryMethod;
import java.time.LocalDate;

public class AllocationOption {
    private final Site site;
    private final DeliveryMethod method;
    private final int maxCapacity;
    private final int transportDays;
    private final LocalDate arrivalDate;

    public AllocationOption(Site site, DeliveryMethod method, int maxCapacity, int transportDays, LocalDate arrivalDate) {
        this.site = site;
        this.method = method;
        this.maxCapacity = maxCapacity;
        this.transportDays = transportDays;
        this.arrivalDate = arrivalDate;
    }

    public Site getSite() { return site; }
    public DeliveryMethod getMethod() { return method; }
    public int getMaxCapacity() { return maxCapacity; }
    public int getTransportDays() { return transportDays; }
    public LocalDate getArrivalDate() { return arrivalDate; }
}
