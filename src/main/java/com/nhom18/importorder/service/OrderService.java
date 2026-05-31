package com.nhom18.importorder.service;

import com.nhom18.importorder.dao.IImportRequestDAO;
import com.nhom18.importorder.dao.IOrderDAO;
import com.nhom18.importorder.dao.ISiteDAO;
import com.nhom18.importorder.dao.ISiteInventoryDAO;
import com.nhom18.importorder.dao.impl.SQLiteImportRequestDAO;
import com.nhom18.importorder.dao.impl.SQLiteOrderDAO;
import com.nhom18.importorder.dao.impl.SQLiteSiteDAO;
import com.nhom18.importorder.dao.impl.SQLiteSiteInventoryDAO;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.enums.OrderStatus;
import java.time.LocalDate;
import java.util.List;

public class OrderService {
    private final OrderQueryService queryService;
    private final OrderProposalService proposalService;
    private final OrderCommandService commandService;

    public OrderService() {
        IOrderDAO orderDAO = new SQLiteOrderDAO();
        IImportRequestDAO requestDAO = new SQLiteImportRequestDAO();
        ISiteInventoryDAO siteInventoryDAO = new SQLiteSiteInventoryDAO();
        ISiteDAO siteDAO = new SQLiteSiteDAO();
        AllocationEngine allocationEngine = new AllocationEngine();

        this.queryService = new OrderQueryService(orderDAO);
        this.proposalService = new OrderProposalService(requestDAO, siteInventoryDAO, siteDAO, allocationEngine);
        this.commandService = new OrderCommandService(orderDAO, requestDAO, siteInventoryDAO, siteDAO, proposalService);
    }

    public OrderService(IOrderDAO orderDAO, IImportRequestDAO requestDAO, ISiteInventoryDAO siteInventoryDAO, ISiteDAO siteDAO, AllocationEngine allocationEngine) {
        this.queryService = new OrderQueryService(orderDAO);
        this.proposalService = new OrderProposalService(requestDAO, siteInventoryDAO, siteDAO, allocationEngine);
        this.commandService = new OrderCommandService(orderDAO, requestDAO, siteInventoryDAO, siteDAO, proposalService);
    }

    public List<Order> getAllOrders() { return queryService.getAllOrders(); }
    public List<Order> getOrdersBySite(String siteCode) { return queryService.getOrdersBySite(siteCode); }
    public List<Order> getOrdersByRequest(int requestId) { return queryService.getOrdersByRequest(requestId); }
    public Order getOrderById(int orderId) { return queryService.getOrderById(orderId); }

    public List<Order> generateProposedOrders(int requestId) { return proposalService.generateProposedOrders(requestId); }
    public List<Order> generateProposedOrders(List<ImportRequestItem> items) { return proposalService.generateProposedOrders(items); }

    public void confirmAndSaveOrders(int requestId, List<Order> orders) { commandService.confirmAndSaveOrders(requestId, orders); }
    public void handleCancelledOrder(int orderId, String reason) { commandService.handleCancelledOrder(orderId, reason); }
    public void updateOrderShipmentStatus(int orderId, OrderStatus status) { commandService.updateOrderShipmentStatus(orderId, status); }
    public void reallocateCancelledOrder(int orderId) { commandService.reallocateCancelledOrder(orderId); }
    public void saveCustomFreeRequestAndOrders(LocalDate desiredDate, List<Order> customOrders) { commandService.saveCustomFreeRequestAndOrders(desiredDate, customOrders); }
}
