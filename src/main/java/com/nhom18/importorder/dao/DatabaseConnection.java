package com.nhom18.importorder.dao;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
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
                // Tuy nhiên, tránh tách nhầm bên trong chuỗi (chúng ta dùng SQLite JDBC nên có thể chạy batch hoặc tách đơn giản)
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
