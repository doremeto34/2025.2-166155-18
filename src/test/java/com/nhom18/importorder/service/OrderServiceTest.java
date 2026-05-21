package com.nhom18.importorder.service;

import static org.junit.jupiter.api.Assertions.*;

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
import com.nhom18.importorder.model.enums.DeliveryMethod;
import com.nhom18.importorder.model.enums.OrderStatus;
import com.nhom18.importorder.model.enums.RequestStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OrderServiceTest {

    private OrderService orderService;
    private MockOrderDAO mockOrderDAO;
    private MockImportRequestDAO mockRequestDAO;
    private MockSiteInventoryDAO mockInventoryDAO;
    private MockSiteDAO mockSiteDAO;

    @BeforeEach
    public void setUp() {
        mockOrderDAO = new MockOrderDAO();
        mockRequestDAO = new MockImportRequestDAO();
        mockInventoryDAO = new MockSiteInventoryDAO();
        mockSiteDAO = new MockSiteDAO();
        AllocationEngine engine = new AllocationEngine();

        orderService = new OrderService(mockOrderDAO, mockRequestDAO, mockInventoryDAO, mockSiteDAO, engine);

        // Setup Seed Sites
        mockSiteDAO.sites.add(new Site("S_TOK", "Tokyo Site", 15, 3, "Japan", true));
        mockSiteDAO.sites.add(new Site("S_SEO", "Seoul Site", 12, 2, "Korea", true));
        mockSiteDAO.sites.add(new Site("S_SIN", "Singapore Site", 7, 1, "Singapore", true));
    }

    @Test
    public void testUpdateOrderShipmentStatusToConfirmed() {
        // Setup Order
        Order order = new Order();
        order.setId(201);
        order.setStatus(OrderStatus.PENDING);
        order.setSiteCode("S_SEO");

        OrderItem item = new OrderItem();
        item.setId(801);
        item.setMerchandiseCode("M_CPU_I7");
        item.setQuantityOrdered(15);
        item.setQuantityConfirmed(0);
        
        order.addItem(item);
        mockOrderDAO.orders.add(order);

        // Update
        orderService.updateOrderShipmentStatus(201, OrderStatus.CONFIRMED);

        // Verify
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(15, item.getQuantityConfirmed(), "Số lượng xác nhận phải tự động bằng số lượng đặt khi chuyển sang CONFIRMED");
    }

    @Test
    public void testReallocateCancelledOrderSuccessfully() {
        // Setup Request
        ImportRequest req = new ImportRequest();
        req.setId(15);
        req.setStatus(RequestStatus.APPROVED);
        
        ImportRequestItem reqItem = new ImportRequestItem();
        reqItem.setMerchandiseCode("M_CPU_I7");
        reqItem.setQuantityOrdered(20);
        reqItem.setDesiredDeliveryDate(LocalDate.now().plusDays(20)); // SHIP is feasible
        req.addItem(reqItem);
        mockRequestDAO.requests.add(req);

        // Setup cancelled order (belonged to S_SEO initially)
        Order cancelledOrder = new Order();
        cancelledOrder.setId(301);
        cancelledOrder.setRequestId(15);
        cancelledOrder.setSiteCode("S_SEO");
        cancelledOrder.setStatus(OrderStatus.CANCELLED);
        cancelledOrder.setEstimatedArrival(LocalDate.now().plusDays(12));

        OrderItem orderItem = new OrderItem();
        orderItem.setId(901);
        orderItem.setMerchandiseCode("M_CPU_I7");
        orderItem.setQuantityOrdered(20);
        orderItem.setUnit("Cái");
        orderItem.setMerchandiseName("Core i7 Processor");
        cancelledOrder.addItem(orderItem);
        mockOrderDAO.orders.add(cancelledOrder);

        // Setup inventory at other sites
        // S_TOK has enough inventory, S_SEO has 0, S_SIN has 0
        mockInventoryDAO.inventories.add(new SiteInventory("S_TOK", "M_CPU_I7", 50, "Cái"));
        mockInventoryDAO.inventories.add(new SiteInventory("S_SEO", "M_CPU_I7", 0, "Cái"));
        mockInventoryDAO.inventories.add(new SiteInventory("S_SIN", "M_CPU_I7", 5, "Cái"));

        // Reallocate
        orderService.reallocateCancelledOrder(301);

        // Verify that a new order is created for Tokyo (S_TOK)
        assertEquals(2, mockOrderDAO.orders.size(), "Một đơn hàng thay thế mới phải được thêm vào CSDL");
        Order newOrder = mockOrderDAO.orders.stream()
            .filter(o -> o.getId() != 301)
            .findFirst()
            .orElse(null);

        assertNotNull(newOrder);
        assertEquals("S_TOK", newOrder.getSiteCode(), "Đơn hàng thay thế phải được giao cho Tokyo");
        assertEquals(OrderStatus.PENDING, newOrder.getStatus(), "Đơn hàng mới phải ở trạng thái PENDING");
        assertEquals(1, newOrder.getItems().size());
        
        OrderItem newItem = newOrder.getItems().get(0);
        assertEquals("M_CPU_I7", newItem.getMerchandiseCode());
        assertEquals(20, newItem.getQuantityOrdered());

        // Verify that stock is deducted at Tokyo
        SiteInventory tokyoInv = mockInventoryDAO.get("S_TOK", "M_CPU_I7");
        assertEquals(30, tokyoInv.getInStockQuantity(), "Tồn kho của Tokyo phải được trừ đi 20 cái (50 - 20 = 30)");

        // Verify that original order's cancel reason is marked [REALLOCATED]
        assertTrue(cancelledOrder.getCancelReason().contains("[REALLOCATED]"), 
            "Lý do hủy đơn hàng cũ phải được cập nhật chứa [REALLOCATED]");
    }

    @Test
    public void testReallocateCancelledOrderThrowsExceptionWhenInsufficientStock() {
        // Setup Request
        ImportRequest req = new ImportRequest();
        req.setId(16);
        ImportRequestItem reqItem = new ImportRequestItem();
        reqItem.setMerchandiseCode("M_CPU_I7");
        reqItem.setQuantityOrdered(30);
        reqItem.setDesiredDeliveryDate(LocalDate.now().plusDays(20));
        req.addItem(reqItem);
        mockRequestDAO.requests.add(req);

        // Setup cancelled order initial S_SEO
        Order cancelledOrder = new Order();
        cancelledOrder.setId(302);
        cancelledOrder.setRequestId(16);
        cancelledOrder.setSiteCode("S_SEO");
        cancelledOrder.setStatus(OrderStatus.CANCELLED);
        cancelledOrder.setEstimatedArrival(LocalDate.now().plusDays(12));

        OrderItem orderItem = new OrderItem();
        orderItem.setMerchandiseCode("M_CPU_I7");
        orderItem.setQuantityOrdered(30);
        orderItem.setUnit("Cái");
        cancelledOrder.addItem(orderItem);
        mockOrderDAO.orders.add(cancelledOrder);

        // Setup insufficient inventory at alternative sites
        mockInventoryDAO.inventories.add(new SiteInventory("S_TOK", "M_CPU_I7", 10, "Cái")); // insufficient
        mockInventoryDAO.inventories.add(new SiteInventory("S_SIN", "M_CPU_I7", 10, "Cái")); // insufficient

        // Verify that it throws exception
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.reallocateCancelledOrder(302);
        }, "Nên ném ngoại lệ do các Site còn lại không đủ tồn kho để phân bổ lại");
    }

    @Test
    public void testReallocateOrderNotCancelledThrowsException() {
        // Setup Order which is still PENDING
        Order pendingOrder = new Order();
        pendingOrder.setId(303);
        pendingOrder.setStatus(OrderStatus.PENDING);
        mockOrderDAO.orders.add(pendingOrder);

        assertThrows(IllegalArgumentException.class, () -> {
            orderService.reallocateCancelledOrder(303);
        }, "Chỉ có thể tái phân bổ đơn hàng có trạng thái CANCELLED");
    }

    @Test
    public void testReallocateAlreadyReallocatedOrderThrowsException() {
        // Setup Order which has already been reallocated
        Order cancelledOrder = new Order();
        cancelledOrder.setId(304);
        cancelledOrder.setStatus(OrderStatus.CANCELLED);
        cancelledOrder.setCancelReason("Site rejected [REALLOCATED]");
        mockOrderDAO.orders.add(cancelledOrder);

        assertThrows(IllegalArgumentException.class, () -> {
            orderService.reallocateCancelledOrder(304);
        }, "Nên ném ngoại lệ khi đơn hàng đã được tái phân bổ trước đó");
    }

    // --- MOCK DAO IMPLEMENTATIONS FOR TESTING ---
    private static class MockOrderDAO implements IOrderDAO {
        List<Order> orders = new ArrayList<>();
        private int idCounter = 1000;

        @Override
        public Order getById(int id) {
            return orders.stream().filter(o -> o.getId() == id).findFirst().orElse(null);
        }

        @Override
        public List<Order> getAll() {
            return orders;
        }

        @Override
        public List<Order> getBySiteCode(String siteCode) {
            return new ArrayList<>();
        }

        @Override
        public List<Order> getByRequestId(int requestId) {
            return new ArrayList<>();
        }

        @Override
        public int insert(Order order) {
            if (order.getId() <= 0) {
                order.setId(++idCounter);
            }
            orders.add(order);
            return order.getId();
        }

        @Override
        public void updateStatus(int orderId, OrderStatus status) {
            Order o = getById(orderId);
            if (o != null) {
                o.setStatus(status);
                // Simulate setting quantityConfirmed equal to quantityOrdered on CONFIRMED
                if (status == OrderStatus.CONFIRMED) {
                    for (OrderItem item : o.getItems()) {
                        item.setQuantityConfirmed(item.getQuantityOrdered());
                    }
                }
            }
        }

        @Override
        public void updateCancelReason(int orderId, String reason) {
            Order o = getById(orderId);
            if (o != null) {
                o.setCancelReason(reason);
                o.setStatus(OrderStatus.CANCELLED);
            }
        }

        @Override
        public void updateItemQuantities(int orderItemId, int confirmedQty, int receivedQty) {
            for (Order o : orders) {
                for (OrderItem item : o.getItems()) {
                    if (item.getId() == orderItemId) {
                        item.setQuantityConfirmed(confirmedQty);
                        item.setQuantityReceived(receivedQty);
                        return;
                    }
                }
            }
        }
    }

    private static class MockImportRequestDAO implements IImportRequestDAO {
        List<ImportRequest> requests = new ArrayList<>();

        @Override
        public ImportRequest getById(int id) {
            return requests.stream().filter(r -> r.getId() == id).findFirst().orElse(null);
        }

        @Override
        public List<ImportRequest> getAllWithCreatorName() {
            return requests;
        }

        @Override
        public int insert(ImportRequest request) {
            requests.add(request);
            return requests.size();
        }

        @Override
        public void insertItem(ImportRequestItem item) {
            ImportRequest req = getById(item.getRequestId());
            if (req != null) {
                req.addItem(item);
            }
        }

        @Override
        public void updateStatus(int requestId, RequestStatus status) {
            ImportRequest r = getById(requestId);
            if (r != null) {
                r.setStatus(status);
            }
        }
    }

    private static class MockSiteInventoryDAO implements ISiteInventoryDAO {
        List<SiteInventory> inventories = new ArrayList<>();

        @Override
        public List<SiteInventory> getByMerchandiseCode(String merchandiseCode) {
            List<SiteInventory> list = new ArrayList<>();
            for (SiteInventory inv : inventories) {
                if (inv.getMerchandiseCode().equals(merchandiseCode)) {
                    list.add(inv);
                }
            }
            return list;
        }

        @Override
        public List<SiteInventory> getBySiteCode(String siteCode) {
            List<SiteInventory> list = new ArrayList<>();
            for (SiteInventory inv : inventories) {
                if (inv.getSiteCode().equals(siteCode)) {
                    list.add(inv);
                }
            }
            return list;
        }

        @Override
        public SiteInventory get(String siteCode, String merchandiseCode) {
            return inventories.stream()
                .filter(i -> i.getSiteCode().equals(siteCode) && i.getMerchandiseCode().equals(merchandiseCode))
                .findFirst()
                .orElse(null);
        }

        @Override
        public void updateStock(String siteCode, String merchandiseCode, int newQuantity) {
            SiteInventory inv = get(siteCode, merchandiseCode);
            if (inv != null) {
                inv.setInStockQuantity(newQuantity);
            }
        }
    }

    private static class MockSiteDAO implements ISiteDAO {
        List<Site> sites = new ArrayList<>();

        @Override
        public Site getByCode(String siteCode) {
            return sites.stream().filter(s -> s.getSiteCode().equals(siteCode)).findFirst().orElse(null);
        }

        @Override
        public List<Site> getAll() {
            return sites;
        }

        @Override
        public List<Site> getAllActive() {
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
