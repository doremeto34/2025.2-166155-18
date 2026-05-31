package com.nhom18.importorder.service;

import static org.junit.jupiter.api.Assertions.*;

import com.nhom18.importorder.dao.IOrderDAO;
import com.nhom18.importorder.dao.IWarehouseReceiptDAO;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.entity.WarehouseReceipt;
import com.nhom18.importorder.model.enums.OrderStatus;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WarehouseServiceTest {

    private WarehouseService warehouseService;
    private MockOrderDAO mockOrderDAO;
    private MockWarehouseReceiptDAO mockReceiptDAO;

    @BeforeEach
    public void setUp() {
        mockOrderDAO = new MockOrderDAO();
        mockReceiptDAO = new MockWarehouseReceiptDAO();
        warehouseService = new WarehouseService(mockOrderDAO, mockReceiptDAO);
    }

    @Test
    public void testConfirmReceiptSuccessfully() {
        // Setup mock order
        Order order = new Order();
        order.setId(100);
        order.setRequestId(10);
        order.setStatus(OrderStatus.SHIPPED);

        OrderItem item = new OrderItem();
        item.setId(500);
        item.setMerchandiseCode("M_CPU_I7");
        item.setQuantityOrdered(10);
        item.setQuantityConfirmed(10);
        item.setQuantityReceived(0);
        item.setUnit("Cái");
        
        List<OrderItem> items = new ArrayList<>();
        items.add(item);
        order.setItems(items);
        
        mockOrderDAO.orders.add(order);

        // Prepare received items updates
        OrderItem receivedItem = new OrderItem();
        receivedItem.setId(500);
        receivedItem.setQuantityConfirmed(10);
        receivedItem.setQuantityReceived(9); // Received 9 instead of 10

        List<OrderItem> itemsToUpdate = new ArrayList<>();
        itemsToUpdate.add(receivedItem);

        // Confirm
        warehouseService.confirmReceipt(100, 8, itemsToUpdate, "Thiếu 1 cái CPU");

        // Verify updates
        assertEquals(OrderStatus.DELIVERED, order.getStatus(), "Trạng thái đơn hàng phải chuyển sang DELIVERED");
        assertEquals(9, item.getQuantityReceived(), "Số lượng thực nhận của mặt hàng phải được cập nhật thành 9");
        
        // Verify warehouse receipt log
        assertEquals(1, mockReceiptDAO.receipts.size(), "Phải tạo đúng 1 phiếu nhập kho");
        WarehouseReceipt receipt = mockReceiptDAO.receipts.get(0);
        assertEquals(100, receipt.getOrderId());
        assertEquals(8, receipt.getConfirmedBy());
        assertEquals("Thiếu 1 cái CPU", receipt.getNotes());
    }

    @Test
    public void testConfirmReceiptWithEmptyItemsThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            warehouseService.confirmReceipt(100, 8, new ArrayList<>(), "Ghi chú");
        });
    }

    // --- MOCK DAO IMPLEMENTATIONS FOR TESTING ---
    private static class MockOrderDAO implements IOrderDAO {
        List<Order> orders = new ArrayList<>();

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
            orders.add(order);
            return order.getId();
        }

        @Override
        public void updateStatus(int orderId, OrderStatus status) {
            Order o = getById(orderId);
            if (o != null) {
                o.setStatus(status);
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

    private static class MockWarehouseReceiptDAO implements IWarehouseReceiptDAO {
        List<WarehouseReceipt> receipts = new ArrayList<>();

        @Override
        public int insert(WarehouseReceipt receipt) {
            receipts.add(receipt);
            receipt.setId(receipts.size());
            return receipt.getId();
        }

        @Override
        public WarehouseReceipt getById(int id) {
            return receipts.stream().filter(r -> r.getId() == id).findFirst().orElse(null);
        }

        @Override
        public List<WarehouseReceipt> getAll() {
            return receipts;
        }

        @Override
        public WarehouseReceipt getByOrderId(int orderId) {
            return receipts.stream().filter(r -> r.getOrderId() == orderId).findFirst().orElse(null);
        }
    }
}
