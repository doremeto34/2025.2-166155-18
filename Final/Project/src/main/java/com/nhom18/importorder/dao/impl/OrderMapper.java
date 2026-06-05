package com.nhom18.importorder.dao.impl;

import com.nhom18.importorder.dao.DatabaseConnection;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.enums.DeliveryMethod;
import com.nhom18.importorder.model.enums.OrderStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrderMapper {

    public static Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        int reqId = rs.getInt("request_id");
        order.setRequestId(rs.wasNull() ? null : reqId);
        order.setSiteCode(rs.getString("site_code"));
        order.setDeliveryMethod(DeliveryMethod.valueOf(rs.getString("delivery_method")));
        order.setStatus(OrderStatus.valueOf(rs.getString("status")));
        order.setCreatedDate(LocalDate.parse(rs.getString("created_date")));
        order.setEstimatedArrival(LocalDate.parse(rs.getString("estimated_arrival")));
        order.setCancelReason(rs.getString("cancel_reason"));
        order.setSiteName(rs.getString("site_name"));
        return order;
    }

    public static List<OrderItem> getOrderItems(int orderId) {
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
                    int srcReqItemId = rs.getInt("source_request_item_id");
                    if (!rs.wasNull()) {
                        item.setSourceRequestItemId(srcReqItemId);
                    }
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
