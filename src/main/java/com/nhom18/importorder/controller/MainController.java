package com.nhom18.importorder.controller;

import com.nhom18.importorder.util.NavigationManager;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class MainController {

    @FXML
    private BorderPane mainContainer;

    @FXML
    private StackPane contentArea;

    @FXML
    private VBox sidebar; // matches fx:id="sidebar" from main.fxml

    private boolean isSidebarShowing = true;

    @FXML
    public void initialize() {
        // Đăng ký vùng chứa hiển thị nội dung chính với NavigationManager
        NavigationManager.getInstance().setMainContentArea(contentArea);
        
        // Điều hướng đến màn hình chào mừng (Welcome) mặc định
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/welcome.fxml");
    }

    @FXML
    private void handleToggleSidebar() {
        if (isSidebarShowing) {
            // Thực hiện hoạt ảnh trượt ẩn sang trái
            TranslateTransition transition = new TranslateTransition(Duration.millis(250), sidebar);
            transition.setFromX(0);
            transition.setToX(-250);
            transition.setOnFinished(e -> {
                mainContainer.setLeft(null);
                isSidebarShowing = false;
            });
            transition.play();
        } else {
            // Đặt lại sidebar vào vùng left của BorderPane
            mainContainer.setLeft(sidebar);
            sidebar.setTranslateX(-250);
            isSidebarShowing = true;
            
            // Thực hiện hoạt ảnh trượt hiển thị từ trái sang phải
            TranslateTransition transition = new TranslateTransition(Duration.millis(250), sidebar);
            transition.setFromX(-250);
            transition.setToX(0);
            transition.play();
        }
    }
}

