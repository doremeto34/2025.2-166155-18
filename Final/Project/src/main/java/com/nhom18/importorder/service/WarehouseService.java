package com.nhom18.importorder.service;

import com.nhom18.importorder.dao.IOrderDAO;
import com.nhom18.importorder.dao.IWarehouseReceiptDAO;
import com.nhom18.importorder.dao.impl.SQLiteOrderDAO;
import com.nhom18.importorder.dao.impl.SQLiteWarehouseReceiptDAO;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.entity.WarehouseReceipt;
import com.nhom18.importorder.model.enums.OrderStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class WarehouseService {
    private final IOrderDAO orderDAO;
    private final IWarehouseReceiptDAO receiptDAO;

    public WarehouseService() {
        this.orderDAO = new SQLiteOrderDAO();
        this.receiptDAO = new SQLiteWarehouseReceiptDAO();
    }

    // Constructor phục vụ cho Unit Tests
    public WarehouseService(IOrderDAO orderDAO, IWarehouseReceiptDAO receiptDAO) {
        this.orderDAO = orderDAO;
        this.receiptDAO = receiptDAO;
    }

    public List<Order> getOrdersPendingReceipt() {
        return orderDAO.getAll().stream()
            .filter(o -> o.getStatus() == OrderStatus.PENDING || 
                         o.getStatus() == OrderStatus.CONFIRMED || 
                         o.getStatus() == OrderStatus.SHIPPED)
            .collect(Collectors.toList());
    }

    public List<Order> getAllOrders() {
        return orderDAO.getAll();
    }

    public WarehouseReceipt getReceiptByOrderId(int orderId) {
        return receiptDAO.getByOrderId(orderId);
    }

    public void confirmReceipt(int orderId, int userId, List<OrderItem> items, String notes) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng không có mặt hàng nào để xác nhận!");
        }

        // 1. Tạo và lưu phiếu nhập kho
        WarehouseReceipt receipt = new WarehouseReceipt();
        receipt.setOrderId(orderId);
        receipt.setConfirmedBy(userId);
        receipt.setConfirmDate(LocalDate.now());
        receipt.setNotes(notes);
        receiptDAO.insert(receipt);

        // 2. Cập nhật số lượng thực nhận và số lượng site xác nhận cho từng dòng mặt hàng
        for (OrderItem item : items) {
            orderDAO.updateItemQuantities(item.getId(), item.getQuantityConfirmed(), item.getQuantityReceived());
        }

        // 3. Cập nhật trạng thái đơn hàng sang DELIVERED (Đã nhập kho / giao thành công)
        orderDAO.updateStatus(orderId, OrderStatus.DELIVERED);
    }
}
