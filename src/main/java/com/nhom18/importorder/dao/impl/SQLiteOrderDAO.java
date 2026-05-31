package com.nhom18.importorder.dao.impl;

import com.nhom18.importorder.dao.DatabaseConnection;
import com.nhom18.importorder.dao.IOrderDAO;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.enums.OrderStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLiteOrderDAO implements IOrderDAO {

    private static final String SELECT_BASE = "SELECT o.*, s.name as site_name, (SELECT iri.request_id FROM order_items oi JOIN import_request_items iri ON oi.source_request_item_id = iri.id WHERE oi.order_id = o.id LIMIT 1) as request_id FROM orders o JOIN sites s ON o.site_code = s.site_code";

    @Override
    public Order getById(int id) {
        String sql = SELECT_BASE + " WHERE o.id = ?";
        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Order order = OrderMapper.mapOrder(rs);
                    order.setItems(OrderMapper.getOrderItems(id));
                    return order;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public List<Order> getAll() {
        List<Order> list = new ArrayList<>();
        String sql = SELECT_BASE + " ORDER BY o.id DESC";
        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Order order = OrderMapper.mapOrder(rs);
                order.setItems(OrderMapper.getOrderItems(order.getId()));
                list.add(order);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public List<Order> getBySiteCode(String siteCode) {
        List<Order> list = new ArrayList<>();
        String sql = SELECT_BASE + " WHERE o.site_code = ? ORDER BY o.id DESC";
        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setString(1, siteCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Order order = OrderMapper.mapOrder(rs);
                    order.setItems(OrderMapper.getOrderItems(order.getId()));
                    list.add(order);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public List<Order> getByRequestId(int requestId) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT DISTINCT o.*, s.name as site_name, ? as request_id FROM orders o JOIN sites s ON o.site_code = s.site_code JOIN order_items oi ON o.id = oi.order_id JOIN import_request_items iri ON o.id = oi.order_id JOIN import_request_items iri2 ON oi.source_request_item_id = iri2.id WHERE iri2.request_id = ? ORDER BY o.id DESC";
        // Let's use the exact original query to ensure 100% logic safety
        String sqlOriginal = "SELECT DISTINCT o.*, s.name as site_name, ? as request_id FROM orders o JOIN sites s ON o.site_code = s.site_code JOIN order_items oi ON o.id = oi.order_id JOIN import_request_items iri ON oi.source_request_item_id = iri.id WHERE iri.request_id = ? ORDER BY o.id DESC";
        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sqlOriginal)) {
            pstmt.setInt(1, requestId);
            pstmt.setInt(2, requestId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Order order = OrderMapper.mapOrder(rs);
                    order.setItems(OrderMapper.getOrderItems(order.getId()));
                    list.add(order);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public int insert(Order order) {
        String sqlOrder = "INSERT INTO orders (site_code, delivery_method, status, created_date, estimated_arrival, cancel_reason) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlItem = "INSERT INTO order_items (order_id, merchandise_code, quantity_ordered, quantity_confirmed, quantity_received, unit, source_request_item_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);
            int orderId = -1;
            try (PreparedStatement pstmtOrder = conn.prepareStatement(sqlOrder, Statement.RETURN_GENERATED_KEYS)) {
                pstmtOrder.setString(1, order.getSiteCode());
                pstmtOrder.setString(2, order.getDeliveryMethod().name());
                pstmtOrder.setString(3, order.getStatus().name());
                pstmtOrder.setString(4, order.getCreatedDate().toString());
                pstmtOrder.setString(5, order.getEstimatedArrival().toString());
                pstmtOrder.setString(6, order.getCancelReason());
                pstmtOrder.executeUpdate();
                try (ResultSet generatedKeys = pstmtOrder.getGeneratedKeys()) {
                    if (generatedKeys.next()) orderId = generatedKeys.getInt(1);
                }
            }
            if (orderId == -1) {
                conn.rollback(); conn.setAutoCommit(true);
                throw new SQLException("Tạo đơn hàng thất bại, không lấy được ID tự sinh.");
            }
            try (PreparedStatement pstmtItem = conn.prepareStatement(sqlItem)) {
                for (OrderItem item : order.getItems()) {
                    pstmtItem.setInt(1, orderId);
                    pstmtItem.setString(2, item.getMerchandiseCode());
                    pstmtItem.setInt(3, item.getQuantityOrdered());
                    pstmtItem.setInt(4, item.getQuantityConfirmed());
                    pstmtItem.setInt(5, item.getQuantityReceived());
                    pstmtItem.setString(6, item.getUnit());
                    if (item.getSourceRequestItemId() != null) pstmtItem.setInt(7, item.getSourceRequestItemId());
                    else pstmtItem.setNull(7, java.sql.Types.INTEGER);
                    pstmtItem.addBatch();
                }
                pstmtItem.executeBatch();
            }
            conn.commit(); conn.setAutoCommit(true);
            order.setId(orderId);
            return orderId;
        } catch (SQLException e) {
            try { conn.rollback(); conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
            throw new RuntimeException("Lỗi lưu đơn hàng vào database: " + e.getMessage());
        }
    }

    @Override
    public void updateStatus(int orderId, OrderStatus status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setInt(2, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void updateCancelReason(int orderId, String reason) {
        String sql = "UPDATE orders SET cancel_reason = ?, status = 'CANCELLED' WHERE id = ?";
        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setString(1, reason);
            pstmt.setInt(2, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void updateItemQuantities(int orderItemId, int confirmedQty, int receivedQty) {
        String sql = "UPDATE order_items SET quantity_confirmed = ?, quantity_received = ? WHERE id = ?";
        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, confirmedQty);
            pstmt.setInt(2, receivedQty);
            pstmt.setInt(3, orderItemId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
