package com.nhom18.importorder.service;

import com.nhom18.importorder.dao.IOrderDAO;
import com.nhom18.importorder.model.entity.Order;
import java.util.List;

public class OrderQueryService {
    private final IOrderDAO orderDAO;

    public OrderQueryService(IOrderDAO orderDAO) {
        this.orderDAO = orderDAO;
    }

    public List<Order> getAllOrders() {
        return orderDAO.getAll();
    }

    public List<Order> getOrdersBySite(String siteCode) {
        return orderDAO.getBySiteCode(siteCode);
    }

    public List<Order> getOrdersByRequest(int requestId) {
        return orderDAO.getByRequestId(requestId);
    }

    public Order getOrderById(int orderId) {
        return orderDAO.getById(orderId);
    }
}
