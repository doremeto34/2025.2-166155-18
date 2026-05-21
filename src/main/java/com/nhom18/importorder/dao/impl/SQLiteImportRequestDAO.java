package com.nhom18.importorder.dao.impl;

import com.nhom18.importorder.dao.DatabaseConnection;
import com.nhom18.importorder.dao.IImportRequestDAO;
import com.nhom18.importorder.model.entity.ImportRequest;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.enums.RequestStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SQLiteImportRequestDAO implements IImportRequestDAO {

    @Override
    public int insert(ImportRequest request) {
        String sql = "INSERT INTO import_requests (created_by, created_date, status) VALUES (?, ?, ?)";
        
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, request.getCreatedBy());
            stmt.setString(2, request.getCreatedDate().toString());
            stmt.setString(3, request.getStatus().name());
            stmt.executeUpdate();
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public void insertItem(ImportRequestItem item) {
        String sql = "INSERT INTO import_request_items (request_id, merchandise_code, quantity_ordered, unit, desired_delivery_date) VALUES (?, ?, ?, ?, ?)";
        
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, item.getRequestId());
            stmt.setString(2, item.getMerchandiseCode());
            stmt.setInt(3, item.getQuantityOrdered());
            stmt.setString(4, item.getUnit());
            stmt.setString(5, item.getDesiredDeliveryDate().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<ImportRequest> getAllWithCreatorName() {
        List<ImportRequest> list = new ArrayList<>();
        String sql = "SELECT r.*, u.full_name as creator_name " +
                     "FROM import_requests r " +
                     "JOIN users u ON r.created_by = u.id " +
                     "ORDER BY r.id DESC";
        
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ImportRequest req = new ImportRequest(
                    rs.getInt("id"),
                    rs.getInt("created_by"),
                    LocalDate.parse(rs.getString("created_date")),
                    RequestStatus.valueOf(rs.getString("status"))
                );
                req.setCreatorName(rs.getString("creator_name"));
                list.add(req);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public ImportRequest getById(int id) {
        String sqlRequest = "SELECT r.*, u.full_name as creator_name " +
                            "FROM import_requests r " +
                            "JOIN users u ON r.created_by = u.id " +
                            "WHERE r.id = ?";
        
        String sqlItems = "SELECT ri.*, m.name as merchandise_name " +
                           "FROM import_request_items ri " +
                           "JOIN merchandise m ON ri.merchandise_code = m.merchandise_code " +
                           "WHERE ri.request_id = ?";
        
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            
            // 1. Get the Request
            PreparedStatement stmtReq = conn.prepareStatement(sqlRequest);
            stmtReq.setInt(1, id);
            ResultSet rsReq = stmtReq.executeQuery();
            
            if (rsReq.next()) {
                ImportRequest req = new ImportRequest(
                    rsReq.getInt("id"),
                    rsReq.getInt("created_by"),
                    LocalDate.parse(rsReq.getString("created_date")),
                    RequestStatus.valueOf(rsReq.getString("status"))
                );
                req.setCreatorName(rsReq.getString("creator_name"));
                
                // 2. Get Request Items
                PreparedStatement stmtItems = conn.prepareStatement(sqlItems);
                stmtItems.setInt(1, id);
                ResultSet rsItems = stmtItems.executeQuery();
                
                while (rsItems.next()) {
                    ImportRequestItem item = new ImportRequestItem(
                        rsItems.getInt("id"),
                        rsItems.getInt("request_id"),
                        rsItems.getString("merchandise_code"),
                        rsItems.getInt("quantity_ordered"),
                        rsItems.getString("unit"),
                        LocalDate.parse(rsItems.getString("desired_delivery_date"))
                    );
                    item.setMerchandiseName(rsItems.getString("merchandise_name"));
                    req.addItem(item);
                }
                return req;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void updateStatus(int requestId, RequestStatus status) {
        String sql = "UPDATE import_requests SET status = ? WHERE id = ?";
        
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, status.name());
            stmt.setInt(2, requestId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
