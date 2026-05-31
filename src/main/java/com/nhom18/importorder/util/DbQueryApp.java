package com.nhom18.importorder.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbQueryApp {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:import_system.db";
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement()) {
                
                System.out.println("=== COMPANY INVENTORY ===");
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM company_inventory")) {
                    while (rs.next()) {
                        System.out.printf("merchandise_code: %s, in_stock_quantity: %d, unit: %s\n",
                                rs.getString("merchandise_code"),
                                rs.getInt("in_stock_quantity"),
                                rs.getString("unit"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
