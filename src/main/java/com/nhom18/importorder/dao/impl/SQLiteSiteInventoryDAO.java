package com.nhom18.importorder.dao.impl;

import com.nhom18.importorder.dao.DatabaseConnection;
import com.nhom18.importorder.dao.ISiteInventoryDAO;
import com.nhom18.importorder.model.entity.SiteInventory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLiteSiteInventoryDAO implements ISiteInventoryDAO {

    @Override
    public List<SiteInventory> getByMerchandiseCode(String merchandiseCode) {
        List<SiteInventory> list = new ArrayList<>();
        String sql = "SELECT * FROM site_inventory WHERE merchandise_code = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, merchandiseCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSiteInventory(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public SiteInventory get(String siteCode, String merchandiseCode) {
        String sql = "SELECT * FROM site_inventory WHERE site_code = ? AND merchandise_code = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, siteCode);
            pstmt.setString(2, merchandiseCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapSiteInventory(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void updateStock(String siteCode, String merchandiseCode, int newQuantity) {
        String sql = "UPDATE site_inventory SET in_stock_quantity = ? WHERE site_code = ? AND merchandise_code = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newQuantity);
            pstmt.setString(2, siteCode);
            pstmt.setString(3, merchandiseCode);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<SiteInventory> getBySiteCode(String siteCode) {
        List<SiteInventory> list = new ArrayList<>();
        String sql = "SELECT si.*, m.name as merchandise_name FROM site_inventory si " +
                     "JOIN merchandise m ON si.merchandise_code = m.merchandise_code " +
                     "WHERE si.site_code = ? " +
                     "ORDER BY si.merchandise_code ASC";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, siteCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    SiteInventory inv = mapSiteInventory(rs);
                    inv.setMerchandiseName(rs.getString("merchandise_name"));
                    list.add(inv);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private SiteInventory mapSiteInventory(ResultSet rs) throws SQLException {
        SiteInventory inv = new SiteInventory();
        inv.setSiteCode(rs.getString("site_code"));
        inv.setMerchandiseCode(rs.getString("merchandise_code"));
        inv.setInStockQuantity(rs.getInt("in_stock_quantity"));
        inv.setUnit(rs.getString("unit"));
        return inv;
    }
}
