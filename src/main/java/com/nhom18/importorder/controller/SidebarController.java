package com.nhom18.importorder.controller;

import com.nhom18.importorder.model.entity.User;
import com.nhom18.importorder.model.enums.UserRole;
import com.nhom18.importorder.util.AlertHelper;
import com.nhom18.importorder.util.NavigationManager;
import com.nhom18.importorder.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SidebarController {

    @FXML
    private Label lblUserFullName;

    @FXML
    private Label lblUserRole;

    @FXML
    private VBox navContainer;

    /* BPBH */
    @FXML
    private Button btnRequestList;
    @FXML
    private Button btnCreateRequest;
    @FXML
    private Button btnMerchandiseList;

    /* BPĐHQT */
    @FXML
    private Button btnRequestProcessing;
    @FXML
    private Button btnOrderList;
    @FXML
    private Button btnSiteList;

    /* SITE */
    @FXML
    private Button btnSiteOrderList;
    @FXML
    private Button btnSiteInventory;

    /* BPQLK */
    @FXML
    private Button btnWarehouseOrderList;

    /* ADMIN */
    @FXML
    private Button btnUserList;

    @FXML
    private Button btnLogout;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            lblUserFullName.setText(user.getFullName());
            lblUserRole.setText(getFriendlyRoleName(user.getRole(), user.getSiteCode()));
            
            // Phân quyền hiển thị các nút chức năng
            configureNavigation(user.getRole());
        }
    }

    private String getFriendlyRoleName(UserRole role, String siteCode) {
        return switch (role) {
            case BPBH -> "Bộ Phận Bán Hàng (BPBH)";
            case BPDHQT -> "Bộ Phận Đặt Hàng Quốc Tế (BPĐHQT)";
            case SITE -> "Đại Diện Site (" + siteCode + ")";
            case BPQLK -> "Bộ Phận Quản Lý Kho (BPQLK)";
            case ADMIN -> "Quản Trị Viên Hệ Thống (ADMIN)";
        };
    }

    private void configureNavigation(UserRole role) {
        // Mặc định ẩn tất cả các nút phân hệ vai trò
        setButtonVisible(btnRequestList, false);
        setButtonVisible(btnCreateRequest, false);
        setButtonVisible(btnMerchandiseList, false);
        
        setButtonVisible(btnRequestProcessing, false);
        setButtonVisible(btnOrderList, false);
        setButtonVisible(btnSiteList, false);
        
        setButtonVisible(btnSiteOrderList, false);
        setButtonVisible(btnSiteInventory, false);
        
        setButtonVisible(btnWarehouseOrderList, false);
        
        setButtonVisible(btnUserList, false);

        // Hiển thị các nút tương ứng với Role đăng nhập
        switch (role) {
            case BPBH -> {
                setButtonVisible(btnRequestList, true);
                setButtonVisible(btnCreateRequest, true);
                setButtonVisible(btnMerchandiseList, true);
            }
            case BPDHQT -> {
                setButtonVisible(btnRequestProcessing, true);
                setButtonVisible(btnOrderList, true);
                setButtonVisible(btnSiteList, true);
            }
            case SITE -> {
                setButtonVisible(btnSiteOrderList, true);
                setButtonVisible(btnSiteInventory, true);
            }
            case BPQLK -> {
                setButtonVisible(btnWarehouseOrderList, true);
            }
            case ADMIN -> {
                setButtonVisible(btnUserList, true);
            }
        }
    }

    private void setButtonVisible(Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
        }
    }

    private void clearActiveStyles() {
        for (javafx.scene.Node node : navContainer.getChildren()) {
            if (node instanceof Button) {
                node.getStyleClass().remove("nav-item-active");
            }
        }
    }

    private void setActiveButton(Button btn) {
        clearActiveStyles();
        if (btn != null) {
            btn.getStyleClass().add("nav-item-active");
        }
    }

    /* ==========================================================================
       XỬ LÝ ĐIỀU HƯỚNG SỰ KIỆN (Navigations placeholders for next modules)
       ========================================================================== */

    @FXML
    private void handleNavigateRequestList() {
        setActiveButton(btnRequestList);
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpbh/request_list.fxml");
    }

    @FXML
    private void handleNavigateCreateRequest() {
        setActiveButton(btnCreateRequest);
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpbh/create_request.fxml");
    }

    @FXML
    private void handleNavigateMerchandiseList() {
        setActiveButton(btnMerchandiseList);
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpbh/merchandise_list.fxml");
    }

    @FXML
    private void handleNavigateRequestProcessing() {
        setActiveButton(btnRequestProcessing);
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpdhqt/request_processing.fxml");
    }

    @FXML
    private void handleNavigateOrderList() {
        setActiveButton(btnOrderList);
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpdhqt/order_list.fxml");
    }

    @FXML
    private void handleNavigateSiteList() {
        setActiveButton(btnSiteList);
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpdhqt/site_list.fxml");
    }

    @FXML
    private void handleNavigateSiteOrderList() {
        setActiveButton(btnSiteOrderList);
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/site/site_order_list.fxml");
    }

    @FXML
    private void handleNavigateSiteInventory() {
        setActiveButton(btnSiteInventory);
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/site/site_inventory.fxml");
    }

    @FXML
    private void handleNavigateWarehouseOrderList() {
        setActiveButton(btnWarehouseOrderList);
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpqlk/warehouse_order_list.fxml");
    }

    @FXML
    private void handleNavigateUserList() {
        setActiveButton(btnUserList);
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/admin/user_list.fxml");
    }

    @FXML
    private void handleLogout() {
        boolean confirm = AlertHelper.showConfirm("Đăng xuất", "Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?");
        if (confirm) {
            SessionManager.getInstance().logout();
            NavigationManager.getInstance().showLoginScreen();
        }
    }
}
