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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class UserDialogController {

    @FXML
    private Label lblTitle;
    @FXML
    private TextField txtUsername;
    @FXML
    private TextField txtFullName;
    @FXML
    private ComboBox<UserRole> cbRole;
    @FXML
    private ComboBox<String> cbSiteCode;
    @FXML
    private Label lblSiteCode;

    @FXML
    private VBox boxPassword;
    @FXML
    private PasswordField txtPassword;

    private final UserService userService;
    private final SiteService siteService;

    private Stage stage;
    private User existingUser;
    private boolean saved = false;

    public UserDialogController() {
        this.userService = new UserService();
        this.siteService = new SiteService();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    public void initialize() {
        // 1. Nạp vai trò vào ComboBox
        cbRole.setItems(FXCollections.observableArrayList(UserRole.values()));
        cbRole.setValue(UserRole.BPBH);

        // 2. Nạp các Site đang hoạt động (Active) vào ComboBox liên kết Site
        try {
            List<Site> activeSites = siteService.getAllActiveSites();
            List<String> siteCodes = activeSites.stream()
                .map(Site::getSiteCode)
                .collect(Collectors.toList());
            cbSiteCode.setItems(FXCollections.observableArrayList(siteCodes));
        } catch (Exception e) {
            cbSiteCode.setItems(FXCollections.observableArrayList());
        }

        // 3. Lắng nghe thay đổi chọn vai trò để ẩn/hiện ComboBox Site tương ứng
        cbRole.valueProperty().addListener((obs, oldRole, newRole) -> {
            boolean isSiteRole = (newRole == UserRole.SITE);
            
            lblSiteCode.setVisible(isSiteRole);
            lblSiteCode.setManaged(isSiteRole);
            
            cbSiteCode.setVisible(isSiteRole);
            cbSiteCode.setManaged(isSiteRole);

            if (!isSiteRole) {
                cbSiteCode.setValue(null);
            }
        });

        // Thiết lập ẩn mặc định khi khởi tạo (vì mặc định là vai trò BPBH)
        lblSiteCode.setVisible(false);
        lblSiteCode.setManaged(false);
        cbSiteCode.setVisible(false);
        cbSiteCode.setManaged(false);
    }

    public void setUser(User user) {
        this.existingUser = user;
        if (user != null) {
            lblTitle.setText("📝 CẬP NHẬT TÀI KHOẢN NHÂN VIÊN");
            
            txtUsername.setText(user.getUsername());
            txtUsername.setDisable(true); // Không cho phép sửa username khi edit
            
            txtFullName.setText(user.getFullName());
            cbRole.setValue(user.getRole());
            
            if (user.getRole() == UserRole.SITE) {
                cbSiteCode.setValue(user.getSiteCode());
            }

            boxPassword.setVisible(false);
            boxPassword.setManaged(false);
        } else {
            lblTitle.setText("➕ THÊM TÀI KHOẢN NHÂN VIÊN");
            
            txtUsername.setDisable(false);
            txtUsername.clear();
            txtFullName.clear();
            
            cbRole.setValue(UserRole.BPBH);
            cbSiteCode.setValue(null);

            boxPassword.setVisible(true);
            boxPassword.setManaged(true);
            txtPassword.clear();
        }
    }

    @FXML
    private void handleSave() {
        String username = txtUsername.getText() != null ? txtUsername.getText().trim() : "";
        String fullName = txtFullName.getText() != null ? txtFullName.getText().trim() : "";
        UserRole role = cbRole.getValue();
        String siteCode = cbSiteCode.getValue();
        String password = txtPassword.getText() != null ? txtPassword.getText().trim() : "";

        // 1. Kiểm tra validate đầu vào
        if (existingUser == null && username.isEmpty()) {
            AlertHelper.showError("Lỗi nhập liệu", "Tên đăng nhập không được để trống!");
            txtUsername.requestFocus();
            return;
        }

        if (existingUser == null && username.contains(" ")) {
            AlertHelper.showError("Lỗi nhập liệu", "Tên đăng nhập không được phép chứa khoảng trắng!");
            txtUsername.requestFocus();
            return;
        }

        if (fullName.isEmpty()) {
            AlertHelper.showError("Lỗi nhập liệu", "Họ và tên nhân viên không được để trống!");
            txtFullName.requestFocus();
            return;
        }

        if (role == null) {
            AlertHelper.showError("Lỗi nhập liệu", "Vui lòng chọn vai trò hệ thống!");
            cbRole.requestFocus();
            return;
        }

        if (role == UserRole.SITE && (siteCode == null || siteCode.isEmpty())) {
            AlertHelper.showError("Lỗi nhập liệu", "Vai trò Đại Diện Site bắt buộc phải chọn Site liên kết!");
            cbSiteCode.requestFocus();
            return;
        }

        if (existingUser == null && password.isEmpty()) {
            AlertHelper.showError("Lỗi nhập liệu", "Mật khẩu ban đầu không được để trống!");
            txtPassword.requestFocus();
            return;
        }

        try {
            if (existingUser == null) {
                // 2. Thêm mới tài khoản
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setFullName(fullName);
                newUser.setRole(role);
                newUser.setSiteCode(role == UserRole.SITE ? siteCode : null);

                userService.createUser(newUser, password);
                AlertHelper.showInfo("Thành công", "Đã thêm mới tài khoản nhân viên '" + fullName + "' thành công!");
            } else {
                // 3. Cập nhật tài khoản
                existingUser.setFullName(fullName);
                existingUser.setRole(role);
                existingUser.setSiteCode(role == UserRole.SITE ? siteCode : null);

                userService.updateUser(existingUser);
                AlertHelper.showInfo("Thành công", "Đã cập nhật tài khoản nhân viên '" + fullName + "' thành công!");
            }

            saved = true;
            if (stage != null) {
                stage.close();
            }
        } catch (IllegalArgumentException e) {
            AlertHelper.showError("Ràng buộc nghiệp vụ", e.getMessage());
        } catch (Exception e) {
            AlertHelper.showError("Lỗi hệ thống", "Không thể lưu thông tin tài khoản:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if (stage != null) {
            stage.close();
        }
    }
}
