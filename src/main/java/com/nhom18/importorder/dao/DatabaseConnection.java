package com.nhom18.importorder.dao;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:import_system.db";

    private DatabaseConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(DB_URL);
            initializeDatabase();
        } catch (Exception e) {
            throw new RuntimeException("Không thể kết nối đến cơ sở dữ liệu: " + e.getMessage());
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        try {
            if (instance == null || instance.getConnection() == null || instance.getConnection().isClosed()) {
                instance = new DatabaseConnection();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return instance;
    }

    public Connection getConnection() { return connection; }

    private void initializeDatabase() {
        try {
            boolean hasOldSchema = false;
            try (Statement s = connection.createStatement()) {
                boolean tableExists = false;
                try (ResultSet rs = s.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='import_request_items'")) {
                    if (rs.next() && rs.getInt(1) > 0) tableExists = true;
                }
                if (tableExists) {
                    boolean hasShortage = false;
                    try (ResultSet rs = s.executeQuery("PRAGMA table_info(import_request_items)")) {
                        while (rs.next()) {
                            if ("quantity_shortage".equals(rs.getString("name"))) {
                                hasShortage = true;
                                break;
                            }
                        }
                    }
                    if (!hasShortage) hasOldSchema = true;
                }
            }

            if (hasOldSchema) {
                try (Statement s = connection.createStatement()) {
                    for (String tbl : new String[]{"warehouse_receipts", "order_items", "orders", "import_request_items", "import_requests", "company_inventory", "site_inventory", "sites", "merchandise", "users"}) {
                        s.executeUpdate("DROP TABLE IF EXISTS " + tbl + ";");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kiểm tra di trú CSDL: " + e.getMessage());
        }

        try (Statement statement = connection.createStatement()) {
            InputStream inputStream = getClass().getResourceAsStream("/db/schema.sql");
            if (inputStream == null) return;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                String sqlContent = reader.lines().collect(Collectors.joining("\n"));
                for (String query : sqlContent.split(";")) {
                    String trimmedQuery = query.trim();
                    if (!trimmedQuery.isEmpty()) statement.addBatch(trimmedQuery);
                }
                statement.executeBatch();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (Statement stmt = connection.createStatement()) {
            String findMissingSql = "SELECT s.site_code, s.name FROM sites s LEFT JOIN users u ON u.site_code = s.site_code WHERE u.id IS NULL AND s.active = 1";
            try (ResultSet rs = stmt.executeQuery(findMissingSql)) {
                List<String[]> missingSites = new ArrayList<>();
                while (rs.next()) missingSites.add(new String[]{rs.getString("site_code"), rs.getString("name")});
                if (!missingSites.isEmpty()) {
                    String insertUserSql = "INSERT INTO users (username, password_hash, full_name, role, site_code, active) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmt = connection.prepareStatement(insertUserSql)) {
                        for (String[] site : missingSites) {
                            pstmt.setString(1, "site_" + site[0].toLowerCase());
                            pstmt.setString(2, "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92");
                            pstmt.setString(3, "Đại diện Site " + site[1]);
                            pstmt.setString(4, "SITE");
                            pstmt.setString(5, site[0]);
                            pstmt.setInt(6, 1);
                            pstmt.addBatch();
                        }
                        pstmt.executeBatch();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đồng bộ tài khoản Site: " + e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
