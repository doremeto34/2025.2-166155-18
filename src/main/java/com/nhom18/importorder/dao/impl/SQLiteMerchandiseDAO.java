package com.nhom18.importorder.dao.impl;

import com.nhom18.importorder.dao.DatabaseConnection;
import com.nhom18.importorder.dao.IMerchandiseDAO;
import com.nhom18.importorder.model.entity.Merchandise;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLiteMerchandiseDAO implements IMerchandiseDAO {

    @Override
    public List<Merchandise> getAllActive() {
        List<Merchandise> list = new ArrayList<>();
        String sql = "SELECT * FROM merchandise WHERE active = 1 ORDER BY merchandise_code ASC";
        
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Merchandise item = new Merchandise(
                    rs.getString("merchandise_code"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("unit"),
                    rs.getDouble("price"),
                    rs.getInt("active") == 1
                );
                list.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Merchandise getByCode(String code) {
        String sql = "SELECT * FROM merchandise WHERE merchandise_code = ?";
        
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new Merchandise(
                    rs.getString("merchandise_code"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("unit"),
                    rs.getDouble("price"),
                    rs.getInt("active") == 1
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Merchandise> getAll() {
        List<Merchandise> list = new ArrayList<>();
        String sql = "SELECT * FROM merchandise ORDER BY merchandise_code ASC";
        
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Merchandise item = new Merchandise(
                    rs.getString("merchandise_code"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("unit"),
                    rs.getDouble("price"),
                    rs.getInt("active") == 1
                );
                list.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void insert(Merchandise merchandise) {
        String sql = "INSERT INTO merchandise (merchandise_code, name, description, unit, price, active) VALUES (?, ?, ?, ?, ?, ?)";
        
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, merchandise.getMerchandiseCode());
            stmt.setString(2, merchandise.getName());
            stmt.setString(3, merchandise.getDescription());
            stmt.setString(4, merchandise.getUnit());
            stmt.setDouble(5, merchandise.getPrice());
            stmt.setInt(6, merchandise.isActive() ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi thêm mặt hàng vào CSDL: " + e.getMessage());
        }
    }

    @Override
    public void update(Merchandise merchandise) {
        String sql = "UPDATE merchandise SET name = ?, description = ?, unit = ?, price = ?, active = ? WHERE merchandise_code = ?";
        
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, merchandise.getName());
            stmt.setString(2, merchandise.getDescription());
            stmt.setString(3, merchandise.getUnit());
            stmt.setDouble(4, merchandise.getPrice());
            stmt.setInt(5, merchandise.isActive() ? 1 : 0);
            stmt.setString(6, merchandise.getMerchandiseCode());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi cập nhật thông tin mặt hàng trong CSDL: " + e.getMessage());
        }
    }

    @Override
    public boolean isUsedInPendingRequests(String code) {
        String sql = "SELECT COUNT(*) FROM import_requests r " +
                     "JOIN import_request_items ri ON r.id = ri.request_id " +
                     "WHERE ri.merchandise_code = ? AND r.status IN ('PENDING', 'PROCESSING')";
        
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

