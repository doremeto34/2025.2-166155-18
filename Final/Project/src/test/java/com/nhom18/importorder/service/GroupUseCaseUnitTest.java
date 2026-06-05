package com.nhom18.importorder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nhom18.importorder.dao.ICompanyInventoryDAO;
import com.nhom18.importorder.dao.IImportRequestDAO;
import com.nhom18.importorder.dao.IOrderDAO;
import com.nhom18.importorder.dao.ISiteDAO;
import com.nhom18.importorder.dao.ISiteInventoryDAO;
import com.nhom18.importorder.dao.IWarehouseReceiptDAO;
import com.nhom18.importorder.model.entity.CompanyInventory;
import com.nhom18.importorder.model.entity.ImportRequest;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.model.entity.SiteInventory;
import com.nhom18.importorder.model.entity.WarehouseReceipt;
import com.nhom18.importorder.model.enums.OrderStatus;
import com.nhom18.importorder.model.enums.RequestStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GroupUseCaseUnitTest {
    private MockOrderDAO orderDAO;
    private MockImportRequestDAO requestDAO;
    private MockSiteInventoryDAO inventoryDAO;
    private MockSiteDAO siteDAO;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderDAO = new MockOrderDAO();
        requestDAO = new MockImportRequestDAO();
        inventoryDAO = new MockSiteInventoryDAO();
        siteDAO = new MockSiteDAO();
        siteDAO.sites.add(new Site("S_TOK", "Tokyo Site", 8, 2, "Japan", true));
        siteDAO.sites.add(new Site("S_SEO", "Seoul Site", 12, 4, "Korea", true));
        orderService = new OrderService(orderDAO, requestDAO, inventoryDAO, siteDAO, new AllocationEngine());
    }

    @Test
    void uc2_viewImportRequestDetail_returnsRequestAndItems() {
        ImportRequest request = createRequest(201, "M_CPU_I7", 5, LocalDate.now().plusDays(12));
        request.setCreatorName("BPBH User");
        requestDAO.requests.add(request);
        ImportRequestService service = new ImportRequestService(requestDAO, new MockCompanyInventoryDAO());

        List<ImportRequest> requests = service.getAllRequests();
        ImportRequest detail = service.getRequestById(201);

        assertEquals(1, requests.size());
        assertNotNull(detail);
        assertEquals("BPBH User", detail.getCreatorName());
        assertEquals("M_CPU_I7", detail.getItems().get(0).getMerchandiseCode());
    }

    @Test
    void uc16_viewOrderDetail_returnsOrderByIdAndOrdersOfRequest() {
        Order order = createOrder(301, 201, "S_TOK", OrderStatus.PENDING, "M_CPU_I7", 5);
        orderDAO.orders.add(order);

        Order detail = orderService.getOrderById(301);
        List<Order> generatedOrders = orderService.getOrdersByRequest(201);

        assertNotNull(detail);
        assertEquals("S_TOK", detail.getSiteCode());
        assertEquals(1, generatedOrders.size());
        assertEquals(5, generatedOrders.get(0).getItems().get(0).getQuantityOrdered());
    }

    @Test
    void uc17_createOrdersFromApprovedDemand_generatesProposalAndSavesOrders() {
        ImportRequest request = createRequest(202, "M_RAM_16G", 6, LocalDate.now().plusDays(20));
        requestDAO.requests.add(request);
        inventoryDAO.inventories.add(new SiteInventory("S_TOK", "M_RAM_16G", 10, "pcs"));

        List<Order> proposedOrders = orderService.generateProposedOrders(202);
        orderService.confirmAndSaveOrders(202, proposedOrders);

        assertEquals(1, orderDAO.insertedOrders.size());
        assertEquals("S_TOK", orderDAO.insertedOrders.get(0).getSiteCode());
        assertEquals(RequestStatus.APPROVED, request.getStatus());
        assertEquals(4, inventoryDAO.get("S_TOK", "M_RAM_16G").getInStockQuantity());
    }

    @Test
    void uc20_reallocateCancelledOrder_createsReplacementOrder() {
        ImportRequest request = createRequest(203, "M_SSD_1T", 3, LocalDate.now().plusDays(20));
        requestDAO.requests.add(request);
        Order cancelledOrder = createOrder(302, 203, "S_SEO", OrderStatus.CANCELLED, "M_SSD_1T", 3);
        cancelledOrder.setCancelReason("Site rejected");
        cancelledOrder.setEstimatedArrival(LocalDate.now().plusDays(15));
        orderDAO.orders.add(cancelledOrder);
        inventoryDAO.inventories.add(new SiteInventory("S_TOK", "M_SSD_1T", 7, "pcs"));

        orderService.reallocateCancelledOrder(302);

        assertEquals(1, orderDAO.insertedOrders.size());
        assertEquals(203, orderDAO.insertedOrders.get(0).getRequestId());
        assertTrue(cancelledOrder.getCancelReason().contains("[REALLOCATED]"));
    }

    @Test
    void uc29_siteViewsAssignedOrdersAndConfirmsOrder() {
        Order tokyoOrder = createOrder(303, 204, "S_TOK", OrderStatus.PENDING, "M_GPU_RTX4070", 2);
        Order seoulOrder = createOrder(304, 205, "S_SEO", OrderStatus.PENDING, "M_GPU_RTX4070", 2);
        orderDAO.orders.add(tokyoOrder);
        orderDAO.orders.add(seoulOrder);

        List<Order> assignedOrders = orderService.getOrdersBySite("S_TOK");
        orderService.updateOrderShipmentStatus(303, OrderStatus.CONFIRMED);

        assertEquals(1, assignedOrders.size());
        assertEquals(OrderStatus.CONFIRMED, tokyoOrder.getStatus());
        assertEquals(2, tokyoOrder.getItems().get(0).getQuantityConfirmed());
    }

    @Test
    void uc33_warehouseConfirmsReceipt_createsReceiptAndMarksDelivered() {
        Order order = createOrder(305, 206, "S_TOK", OrderStatus.SHIPPED, "M_CPU_I7", 4);
        order.getItems().get(0).setQuantityConfirmed(4);
        orderDAO.orders.add(order);
        MockWarehouseReceiptDAO receiptDAO = new MockWarehouseReceiptDAO();
        WarehouseService service = new WarehouseService(orderDAO, receiptDAO);

        OrderItem receivedItem = new OrderItem();
        receivedItem.setId(order.getItems().get(0).getId());
        receivedItem.setQuantityConfirmed(4);
        receivedItem.setQuantityReceived(4);
        service.confirmReceipt(305, 8, List.of(receivedItem), "OK");

        assertEquals(1, receiptDAO.receipts.size());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(4, order.getItems().get(0).getQuantityReceived());
    }

    private static ImportRequest createRequest(int requestId, String merchandiseCode, int quantity, LocalDate desiredDate) {
        ImportRequest request = new ImportRequest();
        request.setId(requestId);
        request.setStatus(RequestStatus.APPROVED);
        ImportRequestItem item = new ImportRequestItem();
        item.setId(requestId + 1000);
        item.setRequestId(requestId);
        item.setMerchandiseCode(merchandiseCode);
        item.setMerchandiseName(merchandiseCode);
        item.setQuantityOrdered(quantity);
        item.setUnit("pcs");
        item.setDesiredDeliveryDate(desiredDate);
        request.addItem(item);
        return request;
    }

    private static Order createOrder(int orderId, int requestId, String siteCode, OrderStatus status, String merchandiseCode, int quantity) {
        Order order = new Order();
        order.setId(orderId);
        order.setRequestId(requestId);
        order.setSiteCode(siteCode);
        order.setStatus(status);
        order.setCreatedDate(LocalDate.now());
        order.setEstimatedArrival(LocalDate.now().plusDays(12));
        OrderItem item = new OrderItem();
        item.setId(orderId + 2000);
        item.setOrderId(orderId);
        item.setMerchandiseCode(merchandiseCode);
        item.setMerchandiseName(merchandiseCode);
        item.setQuantityOrdered(quantity);
        item.setQuantityConfirmed(0);
        item.setQuantityReceived(0);
        item.setUnit("pcs");
        order.addItem(item);
        return order;
    }

    private static class MockOrderDAO implements IOrderDAO {
        private final List<Order> orders = new ArrayList<>();
        private final List<Order> insertedOrders = new ArrayList<>();
        private int nextId = 8000;

        @Override
        public Order getById(int id) {
            return orders.stream().filter(order -> order.getId() == id).findFirst().orElse(null);
        }

        @Override
        public List<Order> getAll() {
            return orders;
        }

        @Override
        public List<Order> getBySiteCode(String siteCode) {
            return orders.stream().filter(order -> siteCode.equals(order.getSiteCode())).toList();
        }

        @Override
        public List<Order> getByRequestId(int requestId) {
            return orders.stream().filter(order -> order.getRequestId() == requestId).toList();
        }

        @Override
        public int insert(Order order) {
            if (order.getId() <= 0) {
                order.setId(++nextId);
            }
            orders.add(order);
            insertedOrders.add(order);
            return order.getId();
        }

        @Override
        public void updateStatus(int orderId, OrderStatus status) {
            Order order = getById(orderId);
            if (order != null) {
                order.setStatus(status);
                if (status == OrderStatus.CONFIRMED) {
                    for (OrderItem item : order.getItems()) {
                        item.setQuantityConfirmed(item.getQuantityOrdered());
                    }
                }
            }
        }

        @Override
        public void updateCancelReason(int orderId, String reason) {
            Order order = getById(orderId);
            if (order != null) {
                order.setCancelReason(reason);
                order.setStatus(OrderStatus.CANCELLED);
            }
        }

        @Override
        public void updateItemQuantities(int orderItemId, int confirmedQty, int receivedQty) {
            for (Order order : orders) {
                for (OrderItem item : order.getItems()) {
                    if (item.getId() == orderItemId) {
                        item.setQuantityConfirmed(confirmedQty);
                        item.setQuantityReceived(receivedQty);
                    }
                }
            }
        }
    }

    private static class MockImportRequestDAO implements IImportRequestDAO {
        private final List<ImportRequest> requests = new ArrayList<>();

        @Override
        public int insert(ImportRequest request) {
            requests.add(request);
            return request.getId();
        }

        @Override
        public void insertItem(ImportRequestItem item) {
        }

        @Override
        public List<ImportRequest> getAllWithCreatorName() {
            return requests;
        }

        @Override
        public ImportRequest getById(int id) {
            return requests.stream().filter(request -> request.getId() == id).findFirst().orElse(null);
        }

        @Override
        public void updateStatus(int requestId, RequestStatus status) {
            ImportRequest request = getById(requestId);
            if (request != null) {
                request.setStatus(status);
            }
        }

        @Override
        public void adjustShortageQuantity(int itemId, int delta) {
        }

        @Override
        public List<ImportRequestItem> getPendingRequestItems() {
            return new ArrayList<>();
        }
    }

    private static class MockSiteInventoryDAO implements ISiteInventoryDAO {
        private final List<SiteInventory> inventories = new ArrayList<>();

        @Override
        public List<SiteInventory> getByMerchandiseCode(String merchandiseCode) {
            return inventories.stream()
                .filter(inventory -> merchandiseCode.equals(inventory.getMerchandiseCode()))
                .toList();
        }

        @Override
        public List<SiteInventory> getBySiteCode(String siteCode) {
            return inventories.stream()
                .filter(inventory -> siteCode.equals(inventory.getSiteCode()))
                .toList();
        }

        @Override
        public SiteInventory get(String siteCode, String merchandiseCode) {
            return inventories.stream()
                .filter(inventory -> siteCode.equals(inventory.getSiteCode())
                    && merchandiseCode.equals(inventory.getMerchandiseCode()))
                .findFirst()
                .orElse(null);
        }

        @Override
        public void updateStock(String siteCode, String merchandiseCode, int newQuantity) {
            SiteInventory inventory = get(siteCode, merchandiseCode);
            if (inventory != null) {
                inventory.setInStockQuantity(newQuantity);
            }
        }
    }

    private static class MockSiteDAO implements ISiteDAO {
        private final List<Site> sites = new ArrayList<>();

        @Override
        public Site getByCode(String siteCode) {
            return sites.stream().filter(site -> siteCode.equals(site.getSiteCode())).findFirst().orElse(null);
        }

        @Override
        public List<Site> getAllActive() {
            return sites.stream().filter(Site::isActive).toList();
        }

        @Override
        public List<Site> getAll() {
            return sites;
        }

        @Override
        public void insert(Site site) {
            sites.add(site);
        }

        @Override
        public void update(Site site) {
        }
    }

    private static class MockCompanyInventoryDAO implements ICompanyInventoryDAO {
        @Override
        public List<CompanyInventory> getAll() {
            return new ArrayList<>();
        }

        @Override
        public CompanyInventory getByMerchandiseCode(String merchandiseCode) {
            return null;
        }

        @Override
        public void updateStock(String merchandiseCode, int newQuantity) {
        }
    }

    private static class MockWarehouseReceiptDAO implements IWarehouseReceiptDAO {
        private final List<WarehouseReceipt> receipts = new ArrayList<>();

        @Override
        public int insert(WarehouseReceipt receipt) {
            receipt.setId(receipts.size() + 1);
            receipts.add(receipt);
            return receipt.getId();
        }

        @Override
        public WarehouseReceipt getById(int id) {
            return receipts.stream().filter(receipt -> receipt.getId() == id).findFirst().orElse(null);
        }

        @Override
        public List<WarehouseReceipt> getAll() {
            return receipts;
        }

        @Override
        public WarehouseReceipt getByOrderId(int orderId) {
            return receipts.stream().filter(receipt -> receipt.getOrderId() == orderId).findFirst().orElse(null);
        }
    }
}
