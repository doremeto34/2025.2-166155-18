package com.nhom18.importorder.controller;

import com.nhom18.importorder.service.AuthService;
import com.nhom18.importorder.util.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblError;

    @FXML
    private Button btnLogin;

    private final AuthService authService;

    public LoginController() {
        this.authService = new AuthService();
    }

    @FXML
    public void initialize() {
        lblError.setVisible(false);
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        lblError.setVisible(false);

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            lblError.setText("Vui lòng điền đầy đủ tên đăng nhập và mật khẩu!");
            lblError.setVisible(true);
            return;
        }

        boolean success = authService.login(username, password);
        if (success) {
            System.out.println("Đăng nhập thành công! Đang chuyển sang màn hình chính...");
            NavigationManager.getInstance().showMainScreen();
        } else {
            lblError.setText("Tên đăng nhập hoặc mật khẩu không chính xác!");
            lblError.setVisible(true);
        }
    }
}
