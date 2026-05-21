package com.nhom18.importorder.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

public class NavigationManager {
    private static NavigationManager instance;
    private Pane mainContentArea;
    private Stage primaryStage;

    private NavigationManager() {}

    public static synchronized NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void setMainContentArea(Pane pane) {
        this.mainContentArea = pane;
    }

    public void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom18/importorder/view/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1100, 700);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            primaryStage.setScene(scene);
            primaryStage.setTitle("Đăng nhập - Hệ thống đặt hàng nhập khẩu");
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Không thể hiển thị màn hình đăng nhập!");
            e.printStackTrace();
        }
    }

    public void showMainScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom18/importorder/view/main.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1500, 900);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            primaryStage.setScene(scene);
            primaryStage.setTitle("Hệ thống quản lý đặt hàng nhập khẩu - Nhóm 18");
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Không thể hiển thị màn hình chính!");
            e.printStackTrace();
        }
    }

    public void navigateTo(String fxmlPath) {
        if (mainContentArea == null) {
            System.err.println("CẢNH BÁO: mainContentArea chưa được thiết lập!");
            return;
        }
        try {
            mainContentArea.getChildren().clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            
            // Căn chỉnh giao diện khớp hoàn toàn vùng content
            view.setStyle("-fx-background-color: transparent;");
            if (view instanceof Pane) {
                Pane viewPane = (Pane) view;
                viewPane.prefWidthProperty().bind(mainContentArea.widthProperty());
                viewPane.prefHeightProperty().bind(mainContentArea.heightProperty());
            }
            
            mainContentArea.getChildren().add(view);
        } catch (IOException e) {
            System.err.println("Lỗi khi điều hướng đến: " + fxmlPath);
            e.printStackTrace();
        }
    }
}
