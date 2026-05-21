package com.nhom18.importorder;

import com.nhom18.importorder.dao.DatabaseConnection;
import com.nhom18.importorder.util.NavigationManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("--- Khởi chạy Hệ thống Đặt Hàng Nhập Khẩu (Nhóm 18) ---");
        
        // 1. Khởi tạo Database Connection (Tự động tạo bảng & Seed data nếu mới hoàn toàn)
        DatabaseConnection.getInstance();

        // 2. Cấu hình Stage cho NavigationManager
        NavigationManager.getInstance().setPrimaryStage(stage);

        // 3. Hiển thị màn hình đăng nhập đầu tiên
        NavigationManager.getInstance().showLoginScreen();

        // 4. Lắng nghe sự kiện đóng ứng dụng để giải phóng tài nguyên database
        stage.setOnCloseRequest(event -> {
            System.out.println("Đang đóng ứng dụng...");
            DatabaseConnection.getInstance().closeConnection();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
