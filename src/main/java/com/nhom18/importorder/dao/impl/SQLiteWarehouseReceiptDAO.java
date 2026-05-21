package com.nhom18.importorder.dao.impl;

import com.nhom18.importorder.dao.DatabaseConnection;
import com.nhom18.importorder.dao.IWarehouseReceiptDAO;
import com.nhom18.importorder.model.entity.WarehouseReceipt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SQLiteWarehouseReceiptDAO implements IWarehouseReceiptDAO {

    @Override
    public int insert(WarehouseReceipt receipt) {
        String sql = "INSERT INTO warehouse_receipts (order_id, confirmed_by, confirm_date, notes) VALUES (?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, receipt.getOrderId());
            pstmt.setInt(2, receipt.getConfirmedBy());
            pstmt.setString(3, receipt.getConfirmDate().toString());
            pstmt.setString(4, receipt.getNotes());
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    receipt.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi lưu phiếu nhập kho: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public WarehouseReceipt getById(int id) {
        String sql = "SELECT wr.*, u.full_name as user_name FROM warehouse_receipts wr " +
                     "JOIN users u ON wr.confirmed_by = u.id " +
                     "WHERE wr.id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapReceipt(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<WarehouseReceipt> getAll() {
        List<WarehouseReceipt> list = new ArrayList<>();
        String sql = "SELECT wr.*, u.full_name as user_name FROM warehouse_receipts wr " +
                     "JOIN users u ON wr.confirmed_by = u.id " +
                     "ORDER BY wr.id DESC";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapReceipt(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public WarehouseReceipt getByOrderId(int orderId) {
        String sql = "SELECT wr.*, u.full_name as user_name FROM warehouse_receipts wr " +
                     "JOIN users u ON wr.confirmed_by = u.id " +
                     "WHERE wr.order_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapReceipt(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private WarehouseReceipt mapReceipt(ResultSet rs) throws SQLException {
        WarehouseReceipt wr = new WarehouseReceipt();
        wr.setId(rs.getInt("id"));
        wr.setOrderId(rs.getInt("order_id"));
        wr.setConfirmedBy(rs.getInt("confirmed_by"));
        wr.setConfirmDate(LocalDate.parse(rs.getString("confirm_date")));
        wr.setNotes(rs.getString("notes"));
        wr.setConfirmedByName(rs.getString("user_name"));
        return wr;
    }
}
