package com.nhom18.importorder.controller.admin;

import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.model.entity.User;
import com.nhom18.importorder.model.enums.UserRole;
import com.nhom18.importorder.service.SiteService;
import com.nhom18.importorder.service.UserService;
import com.nhom18.importorder.util.AlertHelper;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class UserDialogController {

    @FXML private Label lblTitle, lblSiteCode;
    @FXML private TextField txtUsername, txtFullName;
    @FXML private ComboBox<UserRole> cbRole;
    @FXML private ComboBox<String> cbSiteCode;
    @FXML private VBox boxPassword;
    @FXML private PasswordField txtPassword;

    private final UserService userService = new UserService();
    private final SiteService siteService = new SiteService();
    private Stage stage;
    private User existingUser;
    private boolean saved = false;

    public void setStage(Stage stage) { this.stage = stage; }
    public boolean isSaved() { return saved; }

    @FXML
    public void initialize() {
        cbRole.setItems(FXCollections.observableArrayList(UserRole.values()));
        cbRole.setValue(UserRole.BPBH);
        try {
            List<String> siteCodes = siteService.getAllActiveSites().stream().map(Site::getSiteCode).collect(Collectors.toList());
            cbSiteCode.setItems(FXCollections.observableArrayList(siteCodes));
        } catch (Exception e) { cbSiteCode.setItems(FXCollections.observableArrayList()); }

        cbRole.valueProperty().addListener((obs, oldRole, newRole) -> {
            boolean isSite = (newRole == UserRole.SITE);
            toggleSiteVisible(isSite);
            if (!isSite) cbSiteCode.setValue(null);
        });
        toggleSiteVisible(false);
    }

    private void toggleSiteVisible(boolean vis) {
        lblSiteCode.setVisible(vis); lblSiteCode.setManaged(vis);
        cbSiteCode.setVisible(vis); cbSiteCode.setManaged(vis);
    }

    public void setUser(User user) {
        this.existingUser = user;
        if (user != null) {
            lblTitle.setText("📝 CẬP NHẬT TÀI KHOẢN NHÂN VIÊN");
            txtUsername.setText(user.getUsername());
            txtUsername.setDisable(true);
            txtFullName.setText(user.getFullName());
            cbRole.setValue(user.getRole());
            if (user.getRole() == UserRole.SITE) cbSiteCode.setValue(user.getSiteCode());
            togglePasswordVisible(false);
        } else {
            lblTitle.setText("➕ THÊM TÀI KHOẢN NHÂN VIÊN");
            txtUsername.setDisable(false);
            txtUsername.clear(); txtFullName.clear();
            cbRole.setValue(UserRole.BPBH); cbSiteCode.setValue(null);
            togglePasswordVisible(true);
            txtPassword.clear();
        }
    }

    private void togglePasswordVisible(boolean vis) {
        boxPassword.setVisible(vis); boxPassword.setManaged(vis);
    }

    @FXML
    private void handleSave() {
        String username = txtUsername.getText() != null ? txtUsername.getText().trim() : "";
        String fullName = txtFullName.getText() != null ? txtFullName.getText().trim() : "";
        UserRole role = cbRole.getValue();
        String siteCode = cbSiteCode.getValue();
        String password = txtPassword.getText() != null ? txtPassword.getText().trim() : "";

        if (existingUser == null && (username.isEmpty() || username.contains(" "))) {
            AlertHelper.showError("Lỗi", "Tên đăng nhập trống hoặc chứa khoảng trắng!"); return;
        }
        if (fullName.isEmpty()) { AlertHelper.showError("Lỗi", "Họ tên không được trống!"); return; }
        if (role == UserRole.SITE && (siteCode == null || siteCode.isEmpty())) {
            AlertHelper.showError("Lỗi", "Đại diện Site bắt buộc chọn Site!"); return;
        }
        if (existingUser == null && password.isEmpty()) {
            AlertHelper.showError("Lỗi", "Mật khẩu ban đầu không được trống!"); return;
        }

        try {
            if (existingUser == null) {
                User newUser = new User();
                newUser.setUsername(username); newUser.setFullName(fullName);
                newUser.setRole(role); newUser.setSiteCode(role == UserRole.SITE ? siteCode : null);
                userService.createUser(newUser, password);
            } else {
                existingUser.setFullName(fullName); existingUser.setRole(role);
                existingUser.setSiteCode(role == UserRole.SITE ? siteCode : null);
                userService.updateUser(existingUser);
            }
            saved = true;
            if (stage != null) stage.close();
        } catch (Exception e) {
            AlertHelper.showError("Lỗi", e.getMessage());
        }
    }

    @FXML private void handleCancel() { if (stage != null) stage.close(); }
}
