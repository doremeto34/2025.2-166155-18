package com.nhom18.importorder.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

public class AlertHelper {

    public static void showInfo(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        styleAlert(alert);
        alert.showAndWait();
    }

    public static void showWarning(String title, String content) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        styleAlert(alert);
        alert.showAndWait();
    }

    public static void showError(String title, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        styleAlert(alert);
        alert.showAndWait();
    }

    public static boolean showConfirm(String title, String content) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        styleAlert(alert);
        alert.showAndWait();
        return alert.getResult() == ButtonType.OK;
    }

    private static void styleAlert(Alert alert) {
        // Chúng ta có thể load CSS tùy chỉnh cho popup nếu cần
        try {
            alert.getDialogPane().getStylesheets().add(
                AlertHelper.class.getResource("/css/styles.css").toExternalForm()
            );
            alert.getDialogPane().getStyleClass().add("card");
        } catch (Exception e) {
            // Bỏ qua nếu có lỗi tải CSS cho alert
        }
    }
}
