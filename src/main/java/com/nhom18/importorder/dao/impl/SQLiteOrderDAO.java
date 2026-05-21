package com.nhom18.importorder.dao.impl;

import com.nhom18.importorder.dao.DatabaseConnection;
import com.nhom18.importorder.dao.IOrderDAO;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.enums.DeliveryMethod;
import com.nhom18.importorder.model.enums.OrderStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SQLiteOrderDAO implements IOrderDAO {

    @Override
    public Order getById(int id) {
        String sql = "SELECT o.*, s.name as site_name FROM orders o " +
                     "JOIN sites s ON o.site_code = s.site_code " +
                     "WHERE o.id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(getOrderItems(id));
                    return order;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Order> getAll() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, s.name as site_name FROM orders o " +
                     "JOIN sites s ON o.site_code = s.site_code " +
                     "ORDER BY o.id DESC";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Order order = mapOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                list.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Order> getBySiteCode(String siteCode) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, s.name as site_name FROM orders o " +
                     "JOIN sites s ON o.site_code = s.site_code " +
                     "WHERE o.site_code = ? " +
                     "ORDER BY o.id DESC";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, siteCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(getOrderItems(order.getId()));
                    list.add(order);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Order> getByRequestId(int requestId) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, s.name as site_name FROM orders o " +
                     "JOIN sites s ON o.site_code = s.site_code " +
                     "WHERE o.request_id = ? " +
                     "ORDER BY o.id DESC";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, requestId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(getOrderItems(order.getId()));
                    list.add(order);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public int insert(Order order) {
        String sqlOrder = "INSERT INTO orders (request_id, site_code, delivery_method, status, created_date, estimated_arrival, cancel_reason) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlItem = "INSERT INTO order_items (order_id, merchandise_code, quantity_ordered, quantity_confirmed, quantity_received, unit) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        
        try {
            // Tắt auto commit để chạy Transaction an toàn
            conn.setAutoCommit(false);
            
            int orderId = -1;
            try (PreparedStatement pstmtOrder = conn.prepareStatement(sqlOrder, Statement.RETURN_GENERATED_KEYS)) {
                pstmtOrder.setInt(1, order.getRequestId());
                pstmtOrder.setString(2, order.getSiteCode());
                pstmtOrder.setString(3, order.getDeliveryMethod().name());
                pstmtOrder.setString(4, order.getStatus().name());
                pstmtOrder.setString(5, order.getCreatedDate().toString());
                pstmtOrder.setString(6, order.getEstimatedArrival().toString());
                pstmtOrder.setString(7, order.getCancelReason());
                
                pstmtOrder.executeUpdate();
                
                try (ResultSet generatedKeys = pstmtOrder.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        orderId = generatedKeys.getInt(1);
                    }
                }
            }
            
            if (orderId == -1) {
                conn.rollback();
                conn.setAutoCommit(true);
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
                    pstmtItem.addBatch();
                }
                pstmtItem.executeBatch();
            }
            
            conn.commit();
            conn.setAutoCommit(true);
            order.setId(orderId);
            return orderId;
        } catch (SQLException e) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            throw new RuntimeException("Lỗi lưu đơn hàng vào database: " + e.getMessage());
        }
    }

    @Override
    public void updateStatus(int orderId, OrderStatus status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setInt(2, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateCancelReason(int orderId, String reason) {
        String sql = "UPDATE orders SET cancel_reason = ?, status = 'CANCELLED' WHERE id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reason);
            pstmt.setInt(2, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateItemQuantities(int orderItemId, int confirmedQty, int receivedQty) {
        String sql = "UPDATE order_items SET quantity_confirmed = ?, quantity_received = ? WHERE id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, confirmedQty);
            pstmt.setInt(2, receivedQty);
            pstmt.setInt(3, orderItemId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<OrderItem> getOrderItems(int orderId) {
        List<OrderItem> list = new ArrayList<>();
        String sql = "SELECT oi.*, m.name as merchandise_name FROM order_items oi " +
                     "JOIN merchandise m ON oi.merchandise_code = m.merchandise_code " +
                     "WHERE oi.order_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setId(rs.getInt("id"));
                    item.setOrderId(rs.getInt("order_id"));
                    item.setMerchandiseCode(rs.getString("merchandise_code"));
                    item.setQuantityOrdered(rs.getInt("quantity_ordered"));
                    item.setQuantityConfirmed(rs.getInt("quantity_confirmed"));
                    item.setQuantityReceived(rs.getInt("quantity_received"));
                    item.setUnit(rs.getString("unit"));
                    item.setMerchandiseName(rs.getString("merchandise_name"));
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setRequestId(rs.getInt("request_id"));
        order.setSiteCode(rs.getString("site_code"));
        order.setDeliveryMethod(DeliveryMethod.valueOf(rs.getString("delivery_method")));
        order.setStatus(OrderStatus.valueOf(rs.getString("status")));
        order.setCreatedDate(LocalDate.parse(rs.getString("created_date")));
        order.setEstimatedArrival(LocalDate.parse(rs.getString("estimated_arrival")));
        order.setCancelReason(rs.getString("cancel_reason"));
        order.setSiteName(rs.getString("site_name"));
        return order;
    }
}
