package com.nhom18.importorder.controller;

import com.nhom18.importorder.model.entity.User;
import com.nhom18.importorder.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class WelcomeController {

    @FXML
    private Label lblWelcomeName;

    @FXML
    private Label lblWelcomeRole;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            lblWelcomeName.setText(user.getFullName());
            String roleText = switch (user.getRole()) {
                case BPBH -> "Bộ Phận Bán Hàng (BPBH)";
                case BPDHQT -> "Bộ Phận Đặt Hàng Quốc Tế (BPĐHQT)";
                case SITE -> "Đại Diện Site Đối Tác (" + user.getSiteCode() + ")";
                case BPQLK -> "Bộ Phận Quản Lý Kho (BPQLK)";
                case ADMIN -> "Quản Trị Viên Hệ Thống (ADMIN)";
            };
            lblWelcomeRole.setText(roleText);
        }
    }
}
