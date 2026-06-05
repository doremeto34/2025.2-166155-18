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

    @FXML private Label lblUserFullName, lblUserRole;
    @FXML private VBox navContainer;

    @FXML private Button btnRequestList, btnCreateRequest, btnMerchandiseList;
    @FXML private Button btnBpbhRequestList, btnCreateFreeRequest, btnOrderList, btnSiteList;
    @FXML private Button btnSiteOrderList, btnSiteInventory;
    @FXML private Button btnWarehouseOrderList, btnCompanyInventory;
    @FXML private Button btnUserList, btnLogout;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            lblUserFullName.setText(user.getFullName());
            lblUserRole.setText(getFriendlyRoleName(user.getRole(), user.getSiteCode()));
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
        for (Button b : new Button[]{btnRequestList, btnCreateRequest, btnMerchandiseList, btnBpbhRequestList, btnCreateFreeRequest, btnOrderList, btnSiteList, btnSiteOrderList, btnSiteInventory, btnWarehouseOrderList, btnCompanyInventory, btnUserList}) {
            setButtonVisible(b, false);
        }

        switch (role) {
            case BPBH -> {
                setButtonVisible(btnRequestList, true);
                setButtonVisible(btnCreateRequest, true);
                setButtonVisible(btnMerchandiseList, true);
            }
            case BPDHQT -> {
                setButtonVisible(btnBpbhRequestList, true);
                setButtonVisible(btnCreateFreeRequest, true);
                setButtonVisible(btnOrderList, true);
                setButtonVisible(btnSiteList, true);
            }
            case SITE -> {
                setButtonVisible(btnSiteOrderList, true);
                setButtonVisible(btnSiteInventory, true);
            }
            case BPQLK -> {
                setButtonVisible(btnWarehouseOrderList, true);
                setButtonVisible(btnCompanyInventory, true);
            }
            case ADMIN -> setButtonVisible(btnUserList, true);
        }
    }

    private void setButtonVisible(Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
        }
    }

    private void setActiveButton(Button btn) {
        for (javafx.scene.Node node : navContainer.getChildren()) {
            if (node instanceof Button) node.getStyleClass().remove("nav-item-active");
        }
        if (btn != null) btn.getStyleClass().add("nav-item-active");
    }

    private void navigate(Button btn, String path) {
        setActiveButton(btn);
        NavigationManager.getInstance().navigateTo(path);
    }

    @FXML private void handleNavigateRequestList() { navigate(btnRequestList, "/com/nhom18/importorder/view/bpbh/request_list.fxml"); }
    @FXML private void handleNavigateCreateRequest() { navigate(btnCreateRequest, "/com/nhom18/importorder/view/bpbh/create_request.fxml"); }
    @FXML private void handleNavigateMerchandiseList() { navigate(btnMerchandiseList, "/com/nhom18/importorder/view/bpbh/merchandise_list.fxml"); }
    @FXML private void handleNavigateBpbhRequestList() { navigate(btnBpbhRequestList, "/com/nhom18/importorder/view/bpdhqt/bpbh_requests.fxml"); }
    @FXML private void handleNavigateCreateFreeRequest() { navigate(btnCreateFreeRequest, "/com/nhom18/importorder/view/bpdhqt/create_free_request.fxml"); }
    @FXML private void handleNavigateOrderList() { navigate(btnOrderList, "/com/nhom18/importorder/view/bpdhqt/order_list.fxml"); }
    @FXML private void handleNavigateSiteList() { navigate(btnSiteList, "/com/nhom18/importorder/view/bpdhqt/site_list.fxml"); }
    @FXML private void handleNavigateSiteOrderList() { navigate(btnSiteOrderList, "/com/nhom18/importorder/view/site/site_order_list.fxml"); }
    @FXML private void handleNavigateSiteInventory() { navigate(btnSiteInventory, "/com/nhom18/importorder/view/site/site_inventory.fxml"); }
    @FXML private void handleNavigateWarehouseOrderList() { navigate(btnWarehouseOrderList, "/com/nhom18/importorder/view/bpqlk/warehouse_order_list.fxml"); }
    @FXML private void handleNavigateCompanyInventory() { navigate(btnCompanyInventory, "/com/nhom18/importorder/view/bpqlk/company_inventory.fxml"); }
    @FXML private void handleNavigateUserList() { navigate(btnUserList, "/com/nhom18/importorder/view/admin/user_list.fxml"); }

    @FXML
    private void handleLogout() {
        if (AlertHelper.showConfirm("Đăng xuất", "Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?")) {
            SessionManager.getInstance().logout();
            NavigationManager.getInstance().showLoginScreen();
        }
    }
}
