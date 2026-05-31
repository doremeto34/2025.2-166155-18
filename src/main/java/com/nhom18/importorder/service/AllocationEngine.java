package com.nhom18.importorder.service;

import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.model.entity.SiteInventory;
import com.nhom18.importorder.model.enums.DeliveryMethod;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllocationEngine {

    /**
     * Phân bổ số lượng mặt hàng yêu cầu giữa các Site có tồn kho phù hợp với ngày giao mong muốn.
     * Áp dụng thuật toán Greedy (Tham lam) theo thứ tự ưu tiên:
     * 1. Ưu tiên phương thức vận chuyển SHIP hơn AIR để tiết kiệm chi phí.
     * 2. Ưu tiên Site có lượng tồn kho lớn hơn đối với mặt hàng đó (để gom gọn số Site).
     * 3. Ưu tiên Site có thời gian vận chuyển nhanh hơn.
     */
    public List<AllocationDetail> allocate(ImportRequestItem item, LocalDate currentDate, List<SiteInventory> inventories, List<Site> sites) {
        List<AllocationOption> feasibleOptions = new ArrayList<>();

        // Bước 1: Thu thập toàn bộ các phương án khả dụng
        for (SiteInventory inventory : inventories) {
            if (inventory.getInStockQuantity() <= 0) {
                continue;
            }

            Site targetSite = null;
            for (Site s : sites) {
                if (s.getSiteCode().equals(inventory.getSiteCode())) {
                    targetSite = s;
                    break;
                }
            }

            if (targetSite == null || !targetSite.isActive()) {
                continue;
            }

            // Kiểm tra phương án vận chuyển bằng TÀU (SHIP)
            LocalDate arrivalShip = currentDate.plusDays(targetSite.getShipDays());
            if (!arrivalShip.isAfter(item.getDesiredDeliveryDate())) {
                feasibleOptions.add(new AllocationOption(
                    targetSite, DeliveryMethod.SHIP, inventory.getInStockQuantity(), targetSite.getShipDays(), arrivalShip
                ));
            }

            // Kiểm tra phương án vận chuyển bằng MÁY BAY (AIR)
            LocalDate arrivalAir = currentDate.plusDays(targetSite.getAirDays());
            if (!arrivalAir.isAfter(item.getDesiredDeliveryDate())) {
                feasibleOptions.add(new AllocationOption(
                    targetSite, DeliveryMethod.AIR, inventory.getInStockQuantity(), targetSite.getAirDays(), arrivalAir
                ));
            }
        }

        // Bước 2: Sắp xếp các phương án khả dụng theo đúng quy tắc nghiệp vụ
        Collections.sort(feasibleOptions, (opt1, opt2) -> {
            if (opt1.getMethod() != opt2.getMethod()) {
                return opt1.getMethod() == DeliveryMethod.SHIP ? -1 : 1;
            }
            if (opt1.getMaxCapacity() != opt2.getMaxCapacity()) {
                return Integer.compare(opt2.getMaxCapacity(), opt1.getMaxCapacity());
            }
            return Integer.compare(opt1.getTransportDays(), opt2.getTransportDays());
        });

        // Bước 3: Phân bổ số lượng theo cơ chế Greedy
        int remainingQuantity = item.getQuantityOrdered();
        List<AllocationDetail> results = new ArrayList<>();
        java.util.Map<String, Integer> siteAllocated = new java.util.HashMap<>();

        for (AllocationOption option : feasibleOptions) {
            if (remainingQuantity <= 0) {
                break;
            }

            String siteCode = option.getSite().getSiteCode();
            int alreadyAllocated = siteAllocated.getOrDefault(siteCode, 0);
            int availableCapacity = option.getMaxCapacity() - alreadyAllocated;

            if (availableCapacity <= 0) {
                continue;
            }

            int taken = Math.min(remainingQuantity, availableCapacity);
            results.add(new AllocationDetail(
                option.getSite(), option.getMethod(), taken, option.getArrivalDate()
            ));

            siteAllocated.put(siteCode, alreadyAllocated + taken);
            remainingQuantity -= taken;
        }

        // Bước 4: Kiểm chứng kết quả
        if (remainingQuantity > 0) {
            throw new IllegalArgumentException("Không đủ tồn kho khả dụng hoặc không đáp ứng kịp thời gian giao hàng mong muốn cho mặt hàng: " 
                + item.getMerchandiseCode() + " (Thiếu: " + remainingQuantity + " " + item.getUnit() + ")");
        }

        return results;
    }
}
