package com.nhom18.importorder.dao;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.enums.OrderStatus;
import java.util.List;

public interface IOrderDAO {
    Order getById(int id);
    List<Order> getAll();
    List<Order> getBySiteCode(String siteCode);
    List<Order> getByRequestId(int requestId);
    int insert(Order order);
    void updateStatus(int orderId, OrderStatus status);
    void updateCancelReason(int orderId, String reason);
    void updateItemQuantities(int orderItemId, int confirmedQty, int receivedQty);
}
