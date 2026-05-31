package com.nhom18.importorder.dao;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:import_system.db";

    private DatabaseConnection() {
        try {
            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(DB_URL);
            initializeDatabase();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể kết nối đến cơ sở dữ liệu SQLite: " + e.getMessage());
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

    public Connection getConnection() {
        return connection;
    }

    private void initializeDatabase() {
        // 1. Kiểm tra nâng cấp/tái thiết lập Schema nếu phát hiện CSDL cũ
        try {
            boolean hasOldSchema = false;
            try (Statement s = connection.createStatement()) {
                // Kiểm tra xem bảng import_request_items có tồn tại không
                boolean tableExists = false;
                try (ResultSet rs = s.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='import_request_items'")) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        tableExists = true;
                    }
                }
                
                if (tableExists) {
                    // Kiểm tra xem đã có cột quantity_shortage chưa
                    boolean hasShortageColumn = false;
                    try (ResultSet rs = s.executeQuery("PRAGMA table_info(import_request_items)")) {
                        while (rs.next()) {
                            if ("quantity_shortage".equals(rs.getString("name"))) {
                                hasShortageColumn = true;
                                break;
                            }
                        }
                    }
                    if (!hasShortageColumn) {
                        hasOldSchema = true;
                    }
                }
            }

            if (hasOldSchema) {
                System.out.println("CẢNH BÁO: Phát hiện cơ sở dữ liệu cũ. Tiến hành xóa các bảng cũ để tái thiết lập đồng bộ...");
                try (Statement s = connection.createStatement()) {
                    s.executeUpdate("DROP TABLE IF EXISTS warehouse_receipts;");
                    s.executeUpdate("DROP TABLE IF EXISTS order_items;");
                    s.executeUpdate("DROP TABLE IF EXISTS orders;");
                    s.executeUpdate("DROP TABLE IF EXISTS import_request_items;");
                    s.executeUpdate("DROP TABLE IF EXISTS import_requests;");
                    s.executeUpdate("DROP TABLE IF EXISTS company_inventory;");
                    s.executeUpdate("DROP TABLE IF EXISTS site_inventory;");
                    s.executeUpdate("DROP TABLE IF EXISTS sites;");
                    s.executeUpdate("DROP TABLE IF EXISTS merchandise;");
                    s.executeUpdate("DROP TABLE IF EXISTS users;");
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi kiểm tra di trú cơ sở dữ liệu: " + e.getMessage());
        }

        // 2. Chạy schema.sql chuẩn để tái tạo
        try (Statement statement = connection.createStatement()) {
            // Đọc file schema.sql từ resources
            InputStream inputStream = getClass().getResourceAsStream("/db/schema.sql");
            if (inputStream == null) {
                System.err.println("CẢNH BÁO: Không tìm thấy file schema.sql trong resources!");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                String sqlContent = reader.lines().collect(Collectors.joining("\n"));
                
                // Tách các câu lệnh bằng dấu chấm phẩy ;
                String[] queries = sqlContent.split(";");
                for (String query : queries) {
                    String trimmedQuery = query.trim();
                    if (!trimmedQuery.isEmpty()) {
                        statement.addBatch(trimmedQuery);
                    }
                }
                statement.executeBatch();
                System.out.println("Cơ sở dữ liệu SQLite đã được khởi tạo/đồng bộ thành công.");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi khởi tạo cơ sở dữ liệu: " + e.getMessage());
            e.printStackTrace();
        }

        // 3. Tự động đồng bộ tài khoản đăng nhập cho tất cả các Site đang có trong CSDL
        try (Statement stmt = connection.createStatement()) {
            String findMissingSql = "SELECT s.site_code, s.name FROM sites s " +
                                    "LEFT JOIN users u ON u.site_code = s.site_code " +
                                    "WHERE u.id IS NULL AND s.active = 1";
            try (ResultSet rs = stmt.executeQuery(findMissingSql)) {
                java.util.List<String[]> missingSites = new java.util.ArrayList<>();
                while (rs.next()) {
                    missingSites.add(new String[]{rs.getString("site_code"), rs.getString("name")});
                }
                
                if (!missingSites.isEmpty()) {
                    System.out.println("Phát hiện " + missingSites.size() + " Site thiếu tài khoản đăng nhập. Đang tự động bổ sung...");
                    String insertUserSql = "INSERT INTO users (username, password_hash, full_name, role, site_code, active) VALUES (?, ?, ?, ?, ?, ?)";
                    try (java.sql.PreparedStatement pstmt = connection.prepareStatement(insertUserSql)) {
                        for (String[] site : missingSites) {
                            String siteCode = site[0];
                            String siteName = site[1];
                            String username = "site_" + siteCode.toLowerCase();
                            pstmt.setString(1, username);
                            pstmt.setString(2, "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92"); // "123456"
                            pstmt.setString(3, "Đại diện Site " + siteName);
                            pstmt.setString(4, "SITE");
                            pstmt.setString(5, siteCode);
                            pstmt.setInt(6, 1);
                            pstmt.addBatch();
                            System.out.println("Đã tự động tạo tài khoản đăng nhập cho Site: " + siteCode + " (Username: " + username + ")");
                        }
                        pstmt.executeBatch();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tự động đồng bộ tài khoản đăng nhập cho Site: " + e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Kết nối Database SQLite đã được đóng.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
