package com.nhom18.importorder.model.entity;

public class Site {
    private String siteCode;
    private String name;
    private int shipDays;
    private int airDays;
    private String otherInfo;
    private boolean active;

    public Site() {}

    public Site(String siteCode, String name, int shipDays, int airDays, String otherInfo, boolean active) {
        this.siteCode = siteCode;
        this.name = name;
        this.shipDays = shipDays;
        this.airDays = airDays;
        this.otherInfo = otherInfo;
        this.active = active;
    }

    public String getSiteCode() {
        return siteCode;
    }

    public void setSiteCode(String siteCode) {
        this.siteCode = siteCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getShipDays() {
        return shipDays;
    }

    public void setShipDays(int shipDays) {
        this.shipDays = shipDays;
    }

    public int getAirDays() {
        return airDays;
    }

    public void setAirDays(int airDays) {
        this.airDays = airDays;
    }

    public String getOtherInfo() {
        return otherInfo;
    }

    public void setOtherInfo(String otherInfo) {
        this.otherInfo = otherInfo;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return name + " (" + siteCode + ")";
    }
}
