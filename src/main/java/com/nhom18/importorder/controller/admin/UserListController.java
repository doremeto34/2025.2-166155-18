package com.nhom18.importorder.controller.admin;

import com.nhom18.importorder.model.entity.User;
import com.nhom18.importorder.model.enums.UserRole;
import com.nhom18.importorder.service.UserService;
import com.nhom18.importorder.util.AlertHelper;
import com.nhom18.importorder.util.SessionManager;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class UserListController {

    @FXML
    private TableView<User> tblUsers;
    @FXML
    private TableColumn<User, Integer> colId;
    @FXML
    private TableColumn<User, String> colUsername;
    @FXML
    private TableColumn<User, String> colFullName;
    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, String> colSiteCode;
    @FXML
    private TableColumn<User, String> colStatus;

    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cbRoleFilter;
    @FXML
    private ComboBox<String> cbStatusFilter;

    /* Chi tiết tài khoản */
    @FXML
    private Label lblDetailId;
    @FXML
    private Label lblDetailUsername;
    @FXML
    private Label lblDetailFullName;
    @FXML
    private Label lblDetailRole;
    @FXML
    private Label lblDetailStatus;
    @FXML
    private Label lblDetailSiteCode;

    @FXML
    private Button btnEditUser;
    @FXML
    private Button btnChangePassword;
    @FXML
    private Button btnToggleActive;

    private final UserService userService;
    private List<User> allUsersList;

    public UserListController() {
        this.userService = new UserService();
    }

    @FXML
    public void initialize() {
        // 1. Ánh xạ các cột của TableView
        colId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        colUsername.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsername()));
        colFullName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFullName()));
        colRole.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRole().name()));
        colSiteCode.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getSiteCode() != null ? cell.getValue().getSiteCode() : "-"
        ));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().isActive() ? "Đang hoạt động" : "Ngừng hoạt động"
        ));

        // Định dạng cột trạng thái đẹp mắt với badge màu
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    getStyleClass().removeAll("badge", "badge-success", "badge-rejected");
                } else {
                    setText(item);
                    getStyleClass().add("badge");
                    if (item.equals("Đang hoạt động")) {
                        getStyleClass().removeAll("badge-rejected");
                        getStyleClass().add("badge-success");
                        setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                    } else {
                        getStyleClass().removeAll("badge-success");
                        getStyleClass().add("badge-rejected");
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // 2. Thiết lập các ComboBox Bộ Lọc
        cbRoleFilter.setItems(FXCollections.observableArrayList("Tất cả", "BPBH", "BPDHQT", "SITE", "BPQLK", "ADMIN"));
        cbRoleFilter.setValue("Tất cả");

        cbStatusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Đang hoạt động", "Ngừng hoạt động"));
        cbStatusFilter.setValue("Tất cả");

        // Lắng nghe sự kiện bộ lọc & tìm kiếm
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterUsers());
        cbRoleFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterUsers());
        cbStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterUsers());

        // Lắng nghe chọn dòng trên TableView
        tblUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            displayUserDetails(newSelection);
        });

        // Tải dữ liệu ban đầu
        loadUsersData();
        clearDetails();
    }

    private void loadUsersData() {
        try {
            allUsersList = userService.getAllUsers();
            filterUsers();
        } catch (Exception e) {
            AlertHelper.showError("Lỗi hệ thống", "Không thể tải danh sách tài khoản:\n" + e.getMessage());
        }
    }

    private void filterUsers() {
        if (allUsersList == null) return;

        String keyword = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";
        String roleFilter = cbRoleFilter.getValue();
        String statusFilter = cbStatusFilter.getValue();

        List<User> filtered = allUsersList.stream()
            .filter(user -> {
                // 1. Tìm theo từ khóa
                boolean matchesKeyword = keyword.isEmpty() ||
                    user.getUsername().toLowerCase().contains(keyword) ||
                    user.getFullName().toLowerCase().contains(keyword);

                // 2. Lọc theo vai trò
                boolean matchesRole = "Tất cả".equals(roleFilter) || user.getRole().name().equals(roleFilter);

                // 3. Lọc theo trạng thái
                boolean matchesStatus = true;
                if ("Đang hoạt động".equals(statusFilter)) {
                    matchesStatus = user.isActive();
                } else if ("Ngừng hoạt động".equals(statusFilter)) {
                    matchesStatus = !user.isActive();
                }

                return matchesKeyword && matchesRole && matchesStatus;
            })
            .collect(Collectors.toList());

        tblUsers.setItems(FXCollections.observableArrayList(filtered));
    }

    private void displayUserDetails(User user) {
        if (user == null) {
            clearDetails();
            return;
        }

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

        // Bật các nút chức năng
        btnEditUser.setDisable(false);
        btnChangePassword.setDisable(false);

        // Ràng buộc bảo mật: Không cho phép tự ngừng hoạt động chính mình
        User currentLoggedIn = SessionManager.getInstance().getCurrentUser();
        if (currentLoggedIn != null && user.getId() == currentLoggedIn.getId()) {
            btnToggleActive.setDisable(true); // Khóa nút toggle active đối với bản thân
            btnToggleActive.setText("🚫 Không Thể Khóa");
            btnToggleActive.setStyle("");
        } else {
            btnToggleActive.setDisable(false);
        }
    }

    private void clearDetails() {
        lblDetailId.setText("");
        lblDetailUsername.setText("");
        lblDetailFullName.setText("");
        lblDetailRole.setText("");
        lblDetailStatus.setText("");
        lblDetailSiteCode.setText("");

        btnEditUser.setDisable(true);
        btnChangePassword.setDisable(true);
        btnToggleActive.setDisable(true);
        btnToggleActive.setText("🚫 Ngừng Hoạt Động");
        btnToggleActive.setStyle("");
    }

    @FXML
    private void handleRefresh() {
        User selected = tblUsers.getSelectionModel().getSelectedItem();
        loadUsersData();
        if (selected != null) {
            // Cố gắng chọn lại dòng
            for (User u : tblUsers.getItems()) {
                if (u.getId() == selected.getId()) {
                    tblUsers.getSelectionModel().select(u);
                    break;
                }
            }
        } else {
            clearDetails();
        }
    }

    @FXML
    private void handleCreateUser() {
        openUserDialog(null);
    }

    @FXML
    private void handleEditUser() {
        User selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openUserDialog(selected);
        }
    }

    private void openUserDialog(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom18/importorder/view/admin/user_dialog.fxml"));
            Parent root = loader.load();

            UserDialogController controller = loader.getController();
            controller.setUser(user);

            Stage stage = new Stage();
            stage.setTitle(user == null ? "Thêm Mới Tài Khoản Nhân Viên" : "Cập Nhật Tài Khoản Nhân Viên");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(tblUsers.getScene().getWindow());

            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            } catch (Exception e) {
                // ignore
            }

            stage.setScene(scene);
            controller.setStage(stage);

            stage.showAndWait();

            if (controller.isSaved()) {
                loadUsersData();
                if (user != null) {
                    handleRefresh();
                } else {
                    clearDetails();
                }
            }
        } catch (Exception e) {
            AlertHelper.showError("Lỗi hệ thống", "Không thể mở hộp thoại nhập liệu:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChangePassword() {
        User selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom18/importorder/view/admin/change_password_dialog.fxml"));
            Parent root = loader.load();

            ChangePasswordDialogController controller = loader.getController();
            controller.setUser(selected);

            Stage stage = new Stage();
            stage.setTitle("Đặt Lại Mật Khẩu");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(tblUsers.getScene().getWindow());

            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            } catch (Exception e) {
                // ignore
            }

            stage.setScene(scene);
            controller.setStage(stage);

            stage.showAndWait();
        } catch (Exception e) {
            AlertHelper.showError("Lỗi hệ thống", "Không thể mở hộp thoại đổi mật khẩu:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleToggleActive() {
        User selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        User currentLoggedIn = SessionManager.getInstance().getCurrentUser();
        if (currentLoggedIn != null && selected.getId() == currentLoggedIn.getId()) {
            AlertHelper.showError("Bảo mật hệ thống", "Không thể tự vô hiệu hóa tài khoản quản trị đang đăng nhập!");
            return;
        }

        String action = selected.isActive() ? "ngừng hoạt động" : "kích hoạt lại";
        boolean confirm = AlertHelper.showConfirm("Xác nhận thay đổi trạng thái",
            "Bạn có chắc muốn " + action + " tài khoản nhân viên '" + selected.getFullName() + "' không?");

        if (confirm) {
            try {
                userService.toggleUserActiveStatus(selected.getId(), currentLoggedIn != null ? currentLoggedIn.getId() : -1);
                AlertHelper.showInfo("Thành công", "Đã cập nhật trạng thái hoạt động của nhân viên thành công!");
                loadUsersData();
                
                // Chọn lại dòng
                for (User u : tblUsers.getItems()) {
                    if (u.getId() == selected.getId()) {
                        tblUsers.getSelectionModel().select(u);
                        break;
                    }
                }
            } catch (IllegalStateException e) {
                AlertHelper.showError("Ràng buộc bảo mật", e.getMessage());
            } catch (Exception e) {
                AlertHelper.showError("Lỗi hệ thống", "Không thể thay đổi trạng thái hoạt động:\n" + e.getMessage());
            }
        }
    }
}
