package com.nhom18.importorder.dao.impl;

import com.nhom18.importorder.dao.DatabaseConnection;
import com.nhom18.importorder.dao.ISiteDAO;
import com.nhom18.importorder.model.entity.Site;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLiteSiteDAO implements ISiteDAO {

    @Override
    public Site getByCode(String siteCode) {
        String sql = "SELECT * FROM sites WHERE site_code = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, siteCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapSite(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Site> getAllActive() {
        List<Site> list = new ArrayList<>();
        String sql = "SELECT * FROM sites WHERE active = 1";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapSite(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Site> getAll() {
        List<Site> list = new ArrayList<>();
        String sql = "SELECT * FROM sites";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapSite(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void insert(Site site) {
        String sql = "INSERT INTO sites (site_code, name, ship_days, air_days, other_info, active) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, site.getSiteCode());
            pstmt.setString(2, site.getName());
            pstmt.setInt(3, site.getShipDays());
            pstmt.setInt(4, site.getAirDays());
            pstmt.setString(5, site.getOtherInfo());
            pstmt.setInt(6, site.isActive() ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Site site) {
        String sql = "UPDATE sites SET name = ?, ship_days = ?, air_days = ?, other_info = ?, active = ? WHERE site_code = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, site.getName());
            pstmt.setInt(2, site.getShipDays());
            pstmt.setInt(3, site.getAirDays());
            pstmt.setString(4, site.getOtherInfo());
            pstmt.setInt(5, site.isActive() ? 1 : 0);
            pstmt.setString(6, site.getSiteCode());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Site mapSite(ResultSet rs) throws SQLException {
        Site site = new Site();
        site.setSiteCode(rs.getString("site_code"));
        site.setName(rs.getString("name"));
        site.setShipDays(rs.getInt("ship_days"));
        site.setAirDays(rs.getInt("air_days"));
        site.setOtherInfo(rs.getString("other_info"));
        site.setActive(rs.getInt("active") == 1);
        return site;
    }
}
