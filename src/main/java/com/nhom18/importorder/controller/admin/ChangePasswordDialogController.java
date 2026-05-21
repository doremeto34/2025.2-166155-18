package com.nhom18.importorder.controller.admin;

import com.nhom18.importorder.model.entity.User;
import com.nhom18.importorder.service.UserService;
import com.nhom18.importorder.util.AlertHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class ChangePasswordDialogController {

    @FXML
    private Label lblUsername;
    @FXML
    private PasswordField txtNewPassword;
    @FXML
    private PasswordField txtConfirmPassword;

    private final UserService userService;
    private Stage stage;
    private User user;

    public ChangePasswordDialogController() {
        this.userService = new UserService();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            lblUsername.setText(user.getUsername());
        }
    }

    @FXML
    private void handleSave() {
        String newPassword = txtNewPassword.getText() != null ? txtNewPassword.getText().trim() : "";
        String confirmPassword = txtConfirmPassword.getText() != null ? txtConfirmPassword.getText().trim() : "";

        if (newPassword.isEmpty()) {
            AlertHelper.showError("Lỗi nhập liệu", "Mật khẩu mới không được để trống!");
            txtNewPassword.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            AlertHelper.showError("Lỗi nhập liệu", "Vui lòng xác nhận lại mật khẩu mới!");
            txtConfirmPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            AlertHelper.showError("Lỗi nhập liệu", "Mật khẩu xác nhận không khớp với mật khẩu mới!");
            txtConfirmPassword.requestFocus();
            return;
        }

        try {
            userService.changePassword(user.getId(), newPassword);
            AlertHelper.showInfo("Thành công", "Đặt lại mật khẩu cho tài khoản '" + user.getUsername() + "' thành công!");
            if (stage != null) {
                stage.close();
            }
        } catch (IllegalArgumentException e) {
            AlertHelper.showError("Ràng buộc nghiệp vụ", e.getMessage());
        } catch (Exception e) {
            AlertHelper.showError("Lỗi hệ thống", "Không thể cập nhật mật khẩu:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if (stage != null) {
            stage.close();
        }
    }
}
