package com.nhom18.importorder.dao.impl;

import com.nhom18.importorder.dao.DatabaseConnection;
import com.nhom18.importorder.dao.ICompanyInventoryDAO;
import com.nhom18.importorder.model.entity.CompanyInventory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLiteCompanyInventoryDAO implements ICompanyInventoryDAO {

    @Override
    public List<CompanyInventory> getAll() {
        List<CompanyInventory> list = new ArrayList<>();
        String sql = "SELECT ci.*, m.name as merchandise_name FROM company_inventory ci " +
                     "JOIN merchandise m ON ci.merchandise_code = m.merchandise_code " +
                     "ORDER BY ci.merchandise_code ASC";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                CompanyInventory inv = mapCompanyInventory(rs);
                inv.setMerchandiseName(rs.getString("merchandise_name"));
                list.add(inv);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public CompanyInventory getByMerchandiseCode(String merchandiseCode) {
        String sql = "SELECT ci.*, m.name as merchandise_name FROM company_inventory ci " +
                     "JOIN merchandise m ON ci.merchandise_code = m.merchandise_code " +
                     "WHERE ci.merchandise_code = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, merchandiseCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    CompanyInventory inv = mapCompanyInventory(rs);
                    inv.setMerchandiseName(rs.getString("merchandise_name"));
                    return inv;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void updateStock(String merchandiseCode, int newQuantity) {
        String sql = "UPDATE company_inventory SET in_stock_quantity = ? WHERE merchandise_code = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newQuantity);
            pstmt.setString(2, merchandiseCode);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private CompanyInventory mapCompanyInventory(ResultSet rs) throws SQLException {
        CompanyInventory inv = new CompanyInventory();
        inv.setMerchandiseCode(rs.getString("merchandise_code"));
        inv.setInStockQuantity(rs.getInt("in_stock_quantity"));
        inv.setUnit(rs.getString("unit"));
        return inv;
    }
}
