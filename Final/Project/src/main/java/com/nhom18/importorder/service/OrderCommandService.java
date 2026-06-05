package com.nhom18.importorder.service;

import com.nhom18.importorder.dao.IImportRequestDAO;
import com.nhom18.importorder.dao.IOrderDAO;
import com.nhom18.importorder.dao.ISiteDAO;
import com.nhom18.importorder.dao.ISiteInventoryDAO;
import com.nhom18.importorder.model.entity.*;
import com.nhom18.importorder.model.enums.OrderStatus;
import com.nhom18.importorder.model.enums.RequestStatus;
import com.nhom18.importorder.util.SessionManager;
import java.time.LocalDate;
import java.util.*;

public class OrderCommandService {
    private final IOrderDAO orderDAO;
    private final IImportRequestDAO requestDAO;
    private final ISiteInventoryDAO siteInventoryDAO;
    private final ISiteDAO siteDAO;
    private final OrderProposalService proposalService;

    public OrderCommandService(IOrderDAO orderDAO, IImportRequestDAO requestDAO, ISiteInventoryDAO siteInventoryDAO, ISiteDAO siteDAO, OrderProposalService proposalService) {
        this.orderDAO = orderDAO;
        this.requestDAO = requestDAO;
        this.siteInventoryDAO = siteInventoryDAO;
        this.siteDAO = siteDAO;
        this.proposalService = proposalService;
    }

    public void confirmAndSaveOrders(int requestId, List<Order> orders) {
        if (orders == null || orders.isEmpty()) throw new IllegalArgumentException("Danh sách đơn hàng phê duyệt rỗng.");
        for (Order order : orders) {
            orderDAO.insert(order);
            for (OrderItem item : order.getItems()) {
                SiteInventory inv = siteInventoryDAO.get(order.getSiteCode(), item.getMerchandiseCode());
                if (inv != null) {
                    siteInventoryDAO.updateStock(order.getSiteCode(), item.getMerchandiseCode(), Math.max(0, inv.getInStockQuantity() - item.getQuantityOrdered()));
                }
            }
        }
        requestDAO.updateStatus(requestId, RequestStatus.APPROVED);
    }

    public void handleCancelledOrder(int orderId, String reason) {
        Order order = orderDAO.getById(orderId);
        if (order == null) throw new IllegalArgumentException("Không tìm thấy đơn hàng cần hủy có mã: " + orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) return;

        orderDAO.updateCancelReason(orderId, reason);
        for (OrderItem item : order.getItems()) {
            SiteInventory inv = siteInventoryDAO.get(order.getSiteCode(), item.getMerchandiseCode());
            if (inv != null) {
                siteInventoryDAO.updateStock(order.getSiteCode(), item.getMerchandiseCode(), inv.getInStockQuantity() + item.getQuantityOrdered());
            }
            if (item.getSourceRequestItemId() != null) {
                requestDAO.adjustShortageQuantity(item.getSourceRequestItemId(), item.getQuantityOrdered());
            }
        }
    }

    public void updateOrderShipmentStatus(int orderId, OrderStatus status) {
        Order order = orderDAO.getById(orderId);
        if (order == null) throw new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId);
        orderDAO.updateStatus(orderId, status);
        if (status == OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getItems()) {
                orderDAO.updateItemQuantities(item.getId(), item.getQuantityOrdered(), item.getQuantityReceived());
            }
        }
    }

    public void reallocateCancelledOrder(int orderId) {
        Order cancelledOrder = orderDAO.getById(orderId);
        if (cancelledOrder == null) throw new IllegalArgumentException("Không tìm thấy đơn hàng cần tái phân bổ: " + orderId);
        if (cancelledOrder.getStatus() != OrderStatus.CANCELLED) throw new IllegalArgumentException("Chỉ có thể tái phân bổ đơn hàng CANCELLED!");
        if (cancelledOrder.getCancelReason() != null && cancelledOrder.getCancelReason().contains("[REALLOCATED]")) throw new IllegalArgumentException("Đơn hàng đã được tái phân bổ trước đó!");

        ImportRequest request = requestDAO.getById(cancelledOrder.getRequestId());
        List<Site> filteredSites = new ArrayList<>();
        for (Site s : siteDAO.getAllActive()) {
            if (!s.getSiteCode().equals(cancelledOrder.getSiteCode())) filteredSites.add(s);
        }

        List<ImportRequestItem> tempItems = new ArrayList<>();
        for (OrderItem orderItem : cancelledOrder.getItems()) {
            ImportRequestItem tempItem = new ImportRequestItem();
            tempItem.setMerchandiseCode(orderItem.getMerchandiseCode());
            tempItem.setQuantityOrdered(orderItem.getQuantityOrdered());
            tempItem.setUnit(orderItem.getUnit());
            tempItem.setMerchandiseName(orderItem.getMerchandiseName());
            
            LocalDate desiredDate = cancelledOrder.getEstimatedArrival();
            if (request != null) {
                for (ImportRequestItem reqItem : request.getItems()) {
                    if (reqItem.getMerchandiseCode().equals(orderItem.getMerchandiseCode())) {
                        desiredDate = reqItem.getDesiredDeliveryDate();
                        break;
                    }
                }
            }
            tempItem.setDesiredDeliveryDate(desiredDate);
            tempItems.add(tempItem);
        }

        for (Order newOrder : proposalService.generateProposedOrders(tempItems)) {
            newOrder.setRequestId(cancelledOrder.getRequestId());
            orderDAO.insert(newOrder);
            for (OrderItem item : newOrder.getItems()) {
                SiteInventory inv = siteInventoryDAO.get(newOrder.getSiteCode(), item.getMerchandiseCode());
                if (inv != null) {
                    siteInventoryDAO.updateStock(newOrder.getSiteCode(), item.getMerchandiseCode(), Math.max(0, inv.getInStockQuantity() - item.getQuantityOrdered()));
                }
            }
        }
        orderDAO.updateCancelReason(orderId, (cancelledOrder.getCancelReason() != null ? cancelledOrder.getCancelReason() : "") + " [REALLOCATED]");
    }

    public void saveCustomFreeRequestAndOrders(LocalDate desiredDate, List<Order> customOrders) {
        if (customOrders == null || customOrders.isEmpty()) throw new IllegalArgumentException("Danh sách đơn hàng rỗng!");
        
        ImportRequest request = new ImportRequest();
        User currentUser = SessionManager.getInstance().getCurrentUser();
        request.setCreatedBy(currentUser != null ? currentUser.getId() : 9);
        request.setCreatedDate(LocalDate.now());
        request.setStatus(RequestStatus.APPROVED);

        Map<String, ImportRequestItem> requestItemMap = new HashMap<>();
        for (Order order : customOrders) {
            for (OrderItem oi : order.getItems()) {
                String code = oi.getMerchandiseCode();
                ImportRequestItem ri = requestItemMap.computeIfAbsent(code, k -> {
                    ImportRequestItem newItem = new ImportRequestItem();
                    newItem.setMerchandiseCode(code);
                    newItem.setMerchandiseName(oi.getMerchandiseName());
                    newItem.setQuantityOrdered(0);
                    newItem.setQuantityShortage(0);
                    newItem.setUnit(oi.getUnit());
                    newItem.setDesiredDeliveryDate(desiredDate);
                    return newItem;
                });
                ri.setQuantityOrdered(ri.getQuantityOrdered() + oi.getQuantityOrdered());
            }
        }
        
        for (ImportRequestItem ri : requestItemMap.values()) request.addItem(ri);
        int requestId = requestDAO.insert(request);
        request.setId(requestId);
        
        for (ImportRequestItem ri : new ArrayList<>(request.getItems())) {
            ri.setRequestId(requestId);
            requestDAO.insertItem(ri);
        }

        for (Order order : customOrders) {
            order.setRequestId(requestId);
            order.setCreatedDate(LocalDate.now());
            order.setStatus(OrderStatus.PENDING);
            orderDAO.insert(order);

            for (OrderItem item : order.getItems()) {
                if (item.getSourceRequestItemId() != null) {
                    requestDAO.adjustShortageQuantity(item.getSourceRequestItemId(), -item.getQuantityOrdered());
                }
                SiteInventory inv = siteInventoryDAO.get(order.getSiteCode(), item.getMerchandiseCode());
                if (inv != null) {
                    siteInventoryDAO.updateStock(order.getSiteCode(), item.getMerchandiseCode(), Math.max(0, inv.getInStockQuantity() - item.getQuantityOrdered()));
                }
            }
        }
    }
}
