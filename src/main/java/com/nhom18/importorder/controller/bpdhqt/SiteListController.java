package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.model.enums.OrderStatus;
import com.nhom18.importorder.service.OrderService;
import com.nhom18.importorder.service.SiteService;
import com.nhom18.importorder.util.AlertHelper;
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

public class SiteListController {

    @FXML
    private TableView<Site> tblSites;
    @FXML
    private TableColumn<Site, String> colSiteCode;
    @FXML
    private TableColumn<Site, String> colName;
    @FXML
    private TableColumn<Site, Integer> colShipDays;
    @FXML
    private TableColumn<Site, Integer> colAirDays;
    @FXML
    private TableColumn<Site, String> colStatus;

    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cbStatusFilter;

    /* Chi tiết Site */
    @FXML
    private Label lblDetailSiteCode;
    @FXML
    private Label lblDetailName;
    @FXML
    private Label lblDetailShipDays;
    @FXML
    private Label lblDetailAirDays;
    @FXML
    private Label lblDetailStatus;
    @FXML
    private Label lblDetailOtherInfo;

    @FXML
    private Button btnEditSite;
    @FXML
    private Button btnToggleActive;

    /* Lịch sử đơn hàng */
    @FXML
    private TableView<Order> tblOrderHistory;
    @FXML
    private TableColumn<Order, Integer> colOrderId;
    @FXML
    private TableColumn<Order, Integer> colReqId;
    @FXML
    private TableColumn<Order, String> colDeliveryMethod;
    @FXML
    private TableColumn<Order, String> colCreatedDate;
    @FXML
    private TableColumn<Order, String> colEstArrival;
    @FXML
    private TableColumn<Order, String> colOrderStatus;

    private final SiteService siteService;
    private final OrderService orderService;
    private List<Site> allSitesList;

    public SiteListController() {
        this.siteService = new SiteService();
        this.orderService = new OrderService();
    }

    @FXML
    public void initialize() {
        // 1. Ánh xạ các cột trong bảng Site
        colSiteCode.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSiteCode()));
        colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        colShipDays.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getShipDays()).asObject());
        colAirDays.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getAirDays()).asObject());
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isActive() ? "Đang hoạt động" : "Ngừng hoạt động"));

        // Định dạng cột trạng thái Site đẹp mắt
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

        // 2. Ánh xạ cột trong bảng lịch sử đơn hàng
        colOrderId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        colReqId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getRequestId()).asObject());
        colDeliveryMethod.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDeliveryMethod().name()));
        colCreatedDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedDate().toString()));
        colEstArrival.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstimatedArrival().toString()));
        colOrderStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));

        // Định dạng cột trạng thái Order đẹp mắt
        colOrderStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (OrderStatus.valueOf(item)) {
                        case PENDING -> setStyle("-fx-text-fill: #fb923c; -fx-font-weight: bold;"); // Cam
                        case CONFIRMED -> setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;"); // Xanh dương
                        case SHIPPED -> setStyle("-fx-text-fill: #06b6d4; -fx-font-weight: bold;"); // Cyan
                        case DELIVERED -> setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;"); // Xanh lá
                        case CANCELLED -> setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); // Đỏ
                    }
                }
            }
        });

        // 3. Khởi tạo Combobox Lọc Trạng Thái
        cbStatusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Đang hoạt động", "Ngừng hoạt động"));
        cbStatusFilter.setValue("Tất cả");

        // Lắng nghe sự kiện đổi bộ lọc hoặc tìm kiếm
        cbStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterSites());
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterSites());

        // Lắng nghe chọn dòng trong bảng Site
        tblSites.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            displaySiteDetails(newSelection);
        });

        // Tải dữ liệu ban đầu
        loadSitesData();
        clearDetails();
    }

    private void loadSitesData() {
        try {
            allSitesList = siteService.getAllSites();
            filterSites();
        } catch (Exception e) {
            AlertHelper.showError("Lỗi nạp dữ liệu", "Không thể tải danh sách đối tác:\n" + e.getMessage());
        }
    }

    private void filterSites() {
        if (allSitesList == null) return;

        String keyword = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";
        String statusFilter = cbStatusFilter.getValue();

        List<Site> filtered = allSitesList.stream()
            .filter(site -> {
                // Lọc theo từ khóa
                boolean matchesKeyword = keyword.isEmpty() ||
                    site.getSiteCode().toLowerCase().contains(keyword) ||
                    site.getName().toLowerCase().contains(keyword);

                // Lọc theo trạng thái
                boolean matchesStatus = true;
                if ("Đang hoạt động".equals(statusFilter)) {
                    matchesStatus = site.isActive();
                } else if ("Ngừng hoạt động".equals(statusFilter)) {
                    matchesStatus = !site.isActive();
                }

                return matchesKeyword && matchesStatus;
            })
            .collect(Collectors.toList());

        tblSites.setItems(FXCollections.observableArrayList(filtered));
    }

    private void displaySiteDetails(Site site) {
        if (site == null) {
            clearDetails();
            return;
        }

        // Cập nhật thông tin chi tiết
        lblDetailSiteCode.setText(site.getSiteCode());
        lblDetailName.setText(site.getName());
        lblDetailShipDays.setText(site.getShipDays() + " ngày");
        lblDetailAirDays.setText(site.getAirDays() + " ngày");
        lblDetailOtherInfo.setText(site.getOtherInfo() != null && !site.getOtherInfo().trim().isEmpty() 
            ? site.getOtherInfo() : "(Không có thông tin khác)");

        if (site.isActive()) {
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

        // Bật các nút hành động
        btnEditSite.setDisable(false);
        btnToggleActive.setDisable(false);

        // Tải lịch sử đơn hàng của Site đó
        try {
            List<Order> history = orderService.getOrdersBySite(site.getSiteCode());
            tblOrderHistory.setItems(FXCollections.observableArrayList(history));
        } catch (Exception e) {
            tblOrderHistory.setItems(FXCollections.observableArrayList());
            AlertHelper.showError("Lỗi lịch sử đơn", "Không thể tải lịch sử đơn hàng của Site:\n" + e.getMessage());
        }
    }

    private void clearDetails() {
        lblDetailSiteCode.setText("");
        lblDetailName.setText("");
        lblDetailShipDays.setText("");
        lblDetailAirDays.setText("");
        lblDetailStatus.setText("");
        lblDetailOtherInfo.setText("");

        btnEditSite.setDisable(true);
        btnToggleActive.setDisable(true);
        btnToggleActive.setText("🚫 Ngừng Hoạt Động");
        btnToggleActive.setStyle("");

        tblOrderHistory.setItems(FXCollections.observableArrayList());
    }

    @FXML
    private void handleRefresh() {
        Site selected = tblSites.getSelectionModel().getSelectedItem();
        loadSitesData();
        if (selected != null) {
            // Cố gắng chọn lại dòng đã chọn trước đó
            for (Site s : tblSites.getItems()) {
                if (s.getSiteCode().equals(selected.getSiteCode())) {
                    tblSites.getSelectionModel().select(s);
                    break;
                }
            }
        } else {
            clearDetails();
        }
    }

    @FXML
    private void handleCreateSite() {
        openSiteDialog(null);
    }

    @FXML
    private void handleEditSite() {
        Site selected = tblSites.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openSiteDialog(selected);
        }
    }

    private void openSiteDialog(Site site) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom18/importorder/view/bpdhqt/site_dialog.fxml"));
            Parent root = loader.load();

            SiteDialogController controller = loader.getController();
            controller.setSite(site);

            Stage stage = new Stage();
            stage.setTitle(site == null ? "Thêm Đối Tác Site Mới" : "Cập Nhật Thông Tin Đối Tác Site");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(tblSites.getScene().getWindow());

            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            } catch (Exception e) {
                // Bỏ qua nếu không nạp được css
            }

            stage.setScene(scene);
            controller.setStage(stage);

            stage.showAndWait();

            if (controller.isSaved()) {
                loadSitesData();
                // Chọn lại hoặc cập nhật hiển thị dòng
                if (site != null) {
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
    private void handleToggleActive() {
        Site selected = tblSites.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String action = selected.isActive() ? "ngừng hoạt động" : "tái kích hoạt";
        boolean confirm = AlertHelper.showConfirm("Xác nhận thay đổi trạng thái",
            "Bạn có chắc chắn muốn " + action + " đối tác Site '" + selected.getName() + "' không?");

        if (confirm) {
            try {
                siteService.toggleSiteActiveStatus(selected.getSiteCode());
                AlertHelper.showInfo("Thành công", "Đã cập nhật trạng thái hoạt động của đối tác Site thành công!");
                loadSitesData();
                // Chọn lại dòng để hiển thị thông tin mới nhất
                for (Site s : tblSites.getItems()) {
                    if (s.getSiteCode().equals(selected.getSiteCode())) {
                        tblSites.getSelectionModel().select(s);
                        break;
                    }
                }
            } catch (IllegalStateException e) {
                AlertHelper.showError("Ràng buộc nghiệp vụ", e.getMessage());
            } catch (Exception e) {
                AlertHelper.showError("Lỗi", "Không thể cập nhật trạng thái hoạt động:\n" + e.getMessage());
            }
        }
    }
}
