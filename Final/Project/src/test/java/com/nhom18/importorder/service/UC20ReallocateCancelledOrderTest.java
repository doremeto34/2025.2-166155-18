package com.nhom18.importorder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nhom18.importorder.dao.IImportRequestDAO;
import com.nhom18.importorder.dao.IOrderDAO;
import com.nhom18.importorder.dao.ISiteDAO;
import com.nhom18.importorder.dao.ISiteInventoryDAO;
import com.nhom18.importorder.model.entity.ImportRequest;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.model.entity.SiteInventory;
import com.nhom18.importorder.model.enums.OrderStatus;
import com.nhom18.importorder.model.enums.RequestStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UC20ReallocateCancelledOrderTest {
    private static final String MERCHANDISE_CODE = "M_CPU_I7";

    private OrderService orderService;
    private MockOrderDAO orderDAO;
    private MockImportRequestDAO requestDAO;
    private MockSiteInventoryDAO siteInventoryDAO;
    private MockSiteDAO siteDAO;

    @BeforeEach
    void setUp() {
        orderDAO = new MockOrderDAO();
        requestDAO = new MockImportRequestDAO();
        siteInventoryDAO = new MockSiteInventoryDAO();
        siteDAO = new MockSiteDAO();

        siteDAO.sites.add(new Site("S_CANCEL", "Cancelled Site", 10, 3, "Old site", true));
        siteDAO.sites.add(new Site("S_TOK", "Tokyo Site", 8, 2, "Japan", true));
        siteDAO.sites.add(new Site("S_SEO", "Seoul Site", 12, 4, "Korea", true));

        orderService = new OrderService(orderDAO, requestDAO, siteInventoryDAO, siteDAO, new AllocationEngine());
    }

    @Test
    void blackBox_validCancelledOrder_createsReplacementOrderAndMarksOriginal() {
        ImportRequest request = createRequest(1001, MERCHANDISE_CODE, 4, LocalDate.now().plusDays(20));
        requestDAO.requests.add(request);

        Order cancelledOrder = createCancelledOrder(301, 1001, OrderStatus.CANCELLED, "Site rejected", 4);
        orderDAO.orders.add(cancelledOrder);
        siteInventoryDAO.inventories.add(new SiteInventory("S_CANCEL", MERCHANDISE_CODE, 0, "pcs"));
        siteInventoryDAO.inventories.add(new SiteInventory("S_TOK", MERCHANDISE_CODE, 10, "pcs"));

        orderService.reallocateCancelledOrder(301);

        assertEquals(1, orderDAO.insertedOrders.size());
        Order replacementOrder = orderDAO.insertedOrders.get(0);
        assertEquals(1001, replacementOrder.getRequestId());
        assertEquals("S_TOK", replacementOrder.getSiteCode());
        assertEquals(OrderStatus.PENDING, replacementOrder.getStatus());
        assertEquals(1, replacementOrder.getItems().size());
        assertEquals(4, replacementOrder.getItems().get(0).getQuantityOrdered());
        assertEquals(6, siteInventoryDAO.get("S_TOK", MERCHANDISE_CODE).getInStockQuantity());
        assertTrue(cancelledOrder.getCancelReason().contains("[REALLOCATED]"));
    }

    @Test
    void blackBox_unknownOrderId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> orderService.reallocateCancelledOrder(9999));
    }

    @Test
    void blackBox_nonCancelledOrder_throwsIllegalArgumentException() {
        Order pendingOrder = createCancelledOrder(302, 1002, OrderStatus.PENDING, "Waiting", 4);
        orderDAO.orders.add(pendingOrder);

        assertThrows(IllegalArgumentException.class, () -> orderService.reallocateCancelledOrder(302));
    }

    @Test
    void blackBox_alreadyReallocatedOrder_throwsIllegalArgumentException() {
        Order cancelledOrder = createCancelledOrder(303, 1003, OrderStatus.CANCELLED, "Site rejected [REALLOCATED]", 4);
        orderDAO.orders.add(cancelledOrder);

        assertThrows(IllegalArgumentException.class, () -> orderService.reallocateCancelledOrder(303));
    }

    @Test
    void blackBox_insufficientStock_throwsIllegalArgumentException() {
        requestDAO.requests.add(createRequest(1004, MERCHANDISE_CODE, 20, LocalDate.now().plusDays(20)));
        orderDAO.orders.add(createCancelledOrder(304, 1004, OrderStatus.CANCELLED, "Site rejected", 20));
        siteInventoryDAO.inventories.add(new SiteInventory("S_TOK", MERCHANDISE_CODE, 5, "pcs"));
        siteInventoryDAO.inventories.add(new SiteInventory("S_SEO", MERCHANDISE_CODE, 4, "pcs"));

        assertThrows(IllegalArgumentException.class, () -> orderService.reallocateCancelledOrder(304));
        assertEquals(0, orderDAO.insertedOrders.size());
    }

    @Test
    void whiteBox_requestIsMissing_usesEstimatedArrivalFallbackAndMarksReallocated() {
        Order cancelledOrder = createCancelledOrder(305, 2005, OrderStatus.CANCELLED, null, 3);
        cancelledOrder.setEstimatedArrival(LocalDate.now().plusDays(9));
        orderDAO.orders.add(cancelledOrder);
        siteInventoryDAO.inventories.add(new SiteInventory("S_TOK", MERCHANDISE_CODE, 9, "pcs"));

        orderService.reallocateCancelledOrder(305);

        assertEquals(1, orderDAO.insertedOrders.size());
        assertEquals("[REALLOCATED]", cancelledOrder.getCancelReason().trim());
    }

    @Test
    void whiteBox_requestItemDoesNotMatch_usesEstimatedArrivalFallback() {
        ImportRequest request = createRequest(1006, "M_OTHER", 3, LocalDate.now().plusDays(1));
        requestDAO.requests.add(request);

        Order cancelledOrder = createCancelledOrder(306, 1006, OrderStatus.CANCELLED, "Site rejected", 3);
        cancelledOrder.setEstimatedArrival(LocalDate.now().plusDays(9));
        orderDAO.orders.add(cancelledOrder);
        siteInventoryDAO.inventories.add(new SiteInventory("S_TOK", MERCHANDISE_CODE, 9, "pcs"));

        orderService.reallocateCancelledOrder(306);

        assertEquals(1, orderDAO.insertedOrders.size());
        assertTrue(cancelledOrder.getCancelReason().contains("[REALLOCATED]"));
    }

    @Test
    void whiteBox_inventoryLookupReturnsNull_skipsStockUpdateAfterInsert() {
        requestDAO.requests.add(createRequest(1007, MERCHANDISE_CODE, 2, LocalDate.now().plusDays(20)));
        Order cancelledOrder = createCancelledOrder(307, 1007, OrderStatus.CANCELLED, "Site rejected", 2);
        orderDAO.orders.add(cancelledOrder);
        siteInventoryDAO.inventories.add(new SiteInventory("S_TOK", MERCHANDISE_CODE, 8, "pcs"));
        siteInventoryDAO.returnNullFromGet = true;

        orderService.reallocateCancelledOrder(307);

        assertEquals(1, orderDAO.insertedOrders.size());
        assertEquals(0, siteInventoryDAO.updateStockCallCount);
        assertTrue(cancelledOrder.getCancelReason().contains("[REALLOCATED]"));
    }

    private static ImportRequest createRequest(int requestId, String merchandiseCode, int quantity, LocalDate desiredDate) {
        ImportRequest request = new ImportRequest();
        request.setId(requestId);
        request.setStatus(RequestStatus.APPROVED);

        ImportRequestItem item = new ImportRequestItem();
        item.setId(requestId + 5000);
        item.setRequestId(requestId);
        item.setMerchandiseCode(merchandiseCode);
        item.setMerchandiseName("CPU Core i7");
        item.setQuantityOrdered(quantity);
        item.setUnit("pcs");
        item.setDesiredDeliveryDate(desiredDate);
        request.addItem(item);
        return request;
    }

    private static Order createCancelledOrder(int orderId, int requestId, OrderStatus status, String cancelReason, int quantity) {
        Order order = new Order();
        order.setId(orderId);
        order.setRequestId(requestId);
        order.setSiteCode("S_CANCEL");
        order.setStatus(status);
        order.setCancelReason(cancelReason);
        order.setEstimatedArrival(LocalDate.now().plusDays(15));

        OrderItem item = new OrderItem();
        item.setId(orderId + 7000);
        item.setOrderId(orderId);
        item.setMerchandiseCode(MERCHANDISE_CODE);
        item.setMerchandiseName("CPU Core i7");
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
        private int nextId = 9000;

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
        private boolean returnNullFromGet;
        private int updateStockCallCount;

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
            if (returnNullFromGet) {
                return null;
            }
            return inventories.stream()
                .filter(inventory -> siteCode.equals(inventory.getSiteCode())
                    && merchandiseCode.equals(inventory.getMerchandiseCode()))
                .findFirst()
                .orElse(null);
        }

        @Override
        public void updateStock(String siteCode, String merchandiseCode, int newQuantity) {
            updateStockCallCount++;
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
}
