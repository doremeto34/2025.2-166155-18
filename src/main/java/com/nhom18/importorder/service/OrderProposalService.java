package com.nhom18.importorder.service;

import com.nhom18.importorder.dao.IImportRequestDAO;
import com.nhom18.importorder.dao.ISiteDAO;
import com.nhom18.importorder.dao.ISiteInventoryDAO;
import com.nhom18.importorder.model.entity.*;
import com.nhom18.importorder.model.enums.OrderStatus;
import java.time.LocalDate;
import java.util.*;

public class OrderProposalService {
    private final IImportRequestDAO requestDAO;
    private final ISiteInventoryDAO siteInventoryDAO;
    private final ISiteDAO siteDAO;
    private final AllocationEngine allocationEngine;

    public OrderProposalService(IImportRequestDAO requestDAO, ISiteInventoryDAO siteInventoryDAO, ISiteDAO siteDAO, AllocationEngine allocationEngine) {
        this.requestDAO = requestDAO;
        this.siteInventoryDAO = siteInventoryDAO;
        this.siteDAO = siteDAO;
        this.allocationEngine = allocationEngine;
    }

    public List<Order> generateProposedOrders(int requestId) {
        ImportRequest request = requestDAO.getById(requestId);
        if (request == null) throw new IllegalArgumentException("Không tìm thấy yêu cầu nhập hàng có mã ID: " + requestId);
        List<Site> allSites = siteDAO.getAllActive();
        Map<String, Order> orderGroups = new HashMap<>();
        LocalDate currentDate = LocalDate.now();

        for (ImportRequestItem item : request.getItems()) {
            List<SiteInventory> inventories = siteInventoryDAO.getByMerchandiseCode(item.getMerchandiseCode());
            List<AllocationDetail> details = allocationEngine.allocate(item, currentDate, inventories, allSites);

            for (AllocationDetail detail : details) {
                String key = detail.getSite().getSiteCode() + "_" + detail.getMethod().name();
                Order order = orderGroups.get(key);
                if (order == null) {
                    order = new Order();
                    order.setRequestId(requestId);
                    order.setSiteCode(detail.getSite().getSiteCode());
                    order.setSiteName(detail.getSite().getName());
                    order.setDeliveryMethod(detail.getMethod());
                    order.setStatus(OrderStatus.PENDING);
                    order.setCreatedDate(currentDate);
                    order.setEstimatedArrival(detail.getEstimatedArrivalDate());
                    orderGroups.put(key, order);
                }

                if (detail.getEstimatedArrivalDate().isAfter(order.getEstimatedArrival())) {
                    order.setEstimatedArrival(detail.getEstimatedArrivalDate());
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setMerchandiseCode(item.getMerchandiseCode());
                orderItem.setMerchandiseName(item.getMerchandiseName());
                orderItem.setQuantityOrdered(detail.getAllocatedQuantity());
                orderItem.setQuantityConfirmed(0);
                orderItem.setQuantityReceived(0);
                orderItem.setUnit(item.getUnit());
                order.addItem(orderItem);
            }
        }
        return new ArrayList<>(orderGroups.values());
    }

    public List<Order> generateProposedOrders(List<ImportRequestItem> items) {
        List<Site> allSites = siteDAO.getAllActive();
        Map<String, Order> orderGroups = new HashMap<>();
        LocalDate currentDate = LocalDate.now();

        for (ImportRequestItem item : items) {
            List<SiteInventory> inventories = siteInventoryDAO.getByMerchandiseCode(item.getMerchandiseCode());
            List<AllocationDetail> details = allocationEngine.allocate(item, currentDate, inventories, allSites);

            for (AllocationDetail detail : details) {
                String key = detail.getSite().getSiteCode() + "_" + detail.getMethod().name();
                Order order = orderGroups.get(key);
                if (order == null) {
                    order = new Order();
                    order.setRequestId(-1);
                    order.setSiteCode(detail.getSite().getSiteCode());
                    order.setSiteName(detail.getSite().getName());
                    order.setDeliveryMethod(detail.getMethod());
                    order.setStatus(OrderStatus.PENDING);
                    order.setCreatedDate(currentDate);
                    order.setEstimatedArrival(detail.getEstimatedArrivalDate());
                    orderGroups.put(key, order);
                }

                if (detail.getEstimatedArrivalDate().isAfter(order.getEstimatedArrival())) {
                    order.setEstimatedArrival(detail.getEstimatedArrivalDate());
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setMerchandiseCode(item.getMerchandiseCode());
                orderItem.setMerchandiseName(item.getMerchandiseName());
                orderItem.setQuantityOrdered(detail.getAllocatedQuantity());
                orderItem.setQuantityConfirmed(0);
                orderItem.setQuantityReceived(0);
                orderItem.setUnit(item.getUnit());
                if (item.getId() > 0) orderItem.setSourceRequestItemId(item.getId());
                order.addItem(orderItem);
            }
        }
        return new ArrayList<>(orderGroups.values());
    }
}
