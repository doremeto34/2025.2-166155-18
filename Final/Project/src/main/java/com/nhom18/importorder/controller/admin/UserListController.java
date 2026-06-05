package com.nhom18.importorder.controller.admin;

import com.nhom18.importorder.model.entity.User;
import com.nhom18.importorder.service.UserService;
import com.nhom18.importorder.util.AlertHelper;
import com.nhom18.importorder.util.SessionManager;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class UserListController {

    @FXML private TableView<User> tblUsers;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername, colFullName, colRole, colSiteCode, colStatus;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbRoleFilter, cbStatusFilter;

    @FXML private Label lblDetailId, lblDetailUsername, lblDetailFullName, lblDetailRole, lblDetailStatus, lblDetailSiteCode;
    @FXML private Button btnEditUser, btnChangePassword, btnToggleActive;

    private final UserService userService = new UserService();
    private List<User> allUsersList;

    @FXML
    public void initialize() {
        UserTableHelper.setupColumns(colId, colUsername, colFullName, colRole, colSiteCode, colStatus);
        cbRoleFilter.setItems(FXCollections.observableArrayList("Tất cả", "BPBH", "BPDHQT", "SITE", "BPQLK", "ADMIN"));
        cbRoleFilter.setValue("Tất cả");
        cbStatusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Đang hoạt động", "Ngừng hoạt động"));
        cbStatusFilter.setValue("Tất cả");

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterUsers());
        cbRoleFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterUsers());
        cbStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterUsers());
        tblUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> displayUserDetails(newSel));

        loadUsersData();
        clearDetails();
    }

    private void loadUsersData() {
        try {
            allUsersList = userService.getAllUsers();
            filterUsers();
        } catch (Exception e) {
            AlertHelper.showError("Lỗi", "Không tải được danh sách: " + e.getMessage());
        }
    }

    private void filterUsers() {
        if (allUsersList == null) return;
        List<User> filtered = UserTableHelper.filter(allUsersList, txtSearch.getText(), cbRoleFilter.getValue(), cbStatusFilter.getValue());
        tblUsers.setItems(FXCollections.observableArrayList(filtered));
    }

    private void displayUserDetails(User user) {
        if (user == null) { clearDetails(); return; }
        lblDetailId.setText(String.valueOf(user.getId()));
        lblDetailUsername.setText(user.getUsername());
        lblDetailFullName.setText(user.getFullName());
        lblDetailRole.setText(user.getRole().name());
        lblDetailSiteCode.setText(user.getSiteCode() != null ? user.getSiteCode() : "(Không có)");

        if (user.isActive()) {
            lblDetailStatus.setText("ĐANG HOẠT ĐỘNG");
            lblDetailStatus.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
            btnToggleActive.setText("🚫 Ngừng Hoạt Động");
            btnToggleActive.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;");
        } else {
            lblDetailStatus.setText("NGỪNG HOẠT ĐỘNG");
            lblDetailStatus.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            btnToggleActive.setText("✅ Kích Hoạt Lại");
            btnToggleActive.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold;");
        }
        btnEditUser.setDisable(false);
        btnChangePassword.setDisable(false);

        User currentLoggedIn = SessionManager.getInstance().getCurrentUser();
        if (currentLoggedIn != null && user.getId() == currentLoggedIn.getId()) {
            btnToggleActive.setDisable(true);
            btnToggleActive.setText("🚫 Không Thể Khóa");
            btnToggleActive.setStyle("");
        } else btnToggleActive.setDisable(false);
    }

    private void clearDetails() {
        lblDetailId.setText(""); lblDetailUsername.setText(""); lblDetailFullName.setText("");
        lblDetailRole.setText(""); lblDetailStatus.setText(""); lblDetailSiteCode.setText("");
        btnEditUser.setDisable(true); btnChangePassword.setDisable(true); btnToggleActive.setDisable(true);
        btnToggleActive.setText("🚫 Ngừng Hoạt Động"); btnToggleActive.setStyle("");
    }

    @FXML
    private void handleRefresh() {
        User selected = tblUsers.getSelectionModel().getSelectedItem();
        loadUsersData();
        if (selected != null) {
            tblUsers.getItems().stream()
                .filter(u -> u.getId() == selected.getId())
                .findFirst()
                .ifPresent(u -> tblUsers.getSelectionModel().select(u));
        } else clearDetails();
    }

    @FXML private void handleCreateUser() {
        AdminDialogHelper.openUserDialog(tblUsers, null, this::loadUsersData);
        clearDetails();
    }

    @FXML private void handleEditUser() {
        User selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected != null) {
            AdminDialogHelper.openUserDialog(tblUsers, selected, this::handleRefresh);
        }
    }

    @FXML private void handleChangePassword() {
        User selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected != null) AdminDialogHelper.openChangePasswordDialog(tblUsers, selected);
    }

    @FXML
    private void handleToggleActive() {
        User selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        User currentLoggedIn = SessionManager.getInstance().getCurrentUser();
        if (currentLoggedIn != null && selected.getId() == currentLoggedIn.getId()) {
            AlertHelper.showError("Lỗi", "Không thể tự vô hiệu hóa tài khoản đang đăng nhập!"); return;
        }

        String action = selected.isActive() ? "ngừng hoạt động" : "kích hoạt lại";
        if (AlertHelper.showConfirm("Xác nhận", "Bạn có chắc muốn " + action + " tài khoản nhân viên '" + selected.getFullName() + "' không?")) {
            try {
                userService.toggleUserActiveStatus(selected.getId(), currentLoggedIn != null ? currentLoggedIn.getId() : -1);
                AlertHelper.showInfo("Thành công", "Cập nhật trạng thái thành công!");
                loadUsersData();
                tblUsers.getItems().stream()
                    .filter(u -> u.getId() == selected.getId())
                    .findFirst()
                    .ifPresent(u -> tblUsers.getSelectionModel().select(u));
            } catch (Exception e) {
                AlertHelper.showError("Lỗi", e.getMessage());
            }
        }
    }
}
