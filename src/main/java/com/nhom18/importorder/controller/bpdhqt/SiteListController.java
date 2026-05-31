package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.service.OrderService;
import com.nhom18.importorder.service.SiteService;
import com.nhom18.importorder.util.AlertHelper;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SiteListController {

    @FXML private TableView<Site> tblSites;
    @FXML private TableColumn<Site, String> colSiteCode, colName, colStatus;
    @FXML private TableColumn<Site, Integer> colShipDays, colAirDays;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbStatusFilter;

    @FXML private Label lblDetailSiteCode, lblDetailName, lblDetailShipDays, lblDetailAirDays, lblDetailStatus, lblDetailOtherInfo;
    @FXML private Button btnEditSite, btnToggleActive;

    @FXML private TableView<Order> tblOrderHistory;
    @FXML private TableColumn<Order, Integer> colOrderId, colReqId;
    @FXML private TableColumn<Order, String> colDeliveryMethod, colCreatedDate, colEstArrival, colOrderStatus;

    private final SiteService siteService = new SiteService();
    private final OrderService orderService = new OrderService();
    private List<Site> allSitesList;

    @FXML
    public void initialize() {
        SiteTableHelper.setupSiteColumns(colSiteCode, colName, colShipDays, colAirDays, colStatus);
        SiteTableHelper.setupHistoryColumns(colOrderId, colReqId, colDeliveryMethod, colCreatedDate, colEstArrival, colOrderStatus);

        cbStatusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Đang hoạt động", "Ngừng hoạt động"));
        cbStatusFilter.setValue("Tất cả");

        cbStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterSites());
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterSites());
        tblSites.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> displaySiteDetails(newSel));

        loadSitesData();
        clearDetails();
    }

    private void loadSitesData() {
        try {
            allSitesList = siteService.getAllSites();
            filterSites();
        } catch (Exception e) {
            AlertHelper.showError("Lỗi", "Không thể tải danh sách đối tác: " + e.getMessage());
        }
    }

    private void filterSites() {
        if (allSitesList == null) return;
        List<Site> filtered = SiteTableHelper.filter(allSitesList, txtSearch.getText(), cbStatusFilter.getValue());
        tblSites.setItems(FXCollections.observableArrayList(filtered));
    }

    private void displaySiteDetails(Site site) {
        if (site == null) { clearDetails(); return; }
        lblDetailSiteCode.setText(site.getSiteCode());
        lblDetailName.setText(site.getName());
        lblDetailShipDays.setText(site.getShipDays() + " ngày");
        lblDetailAirDays.setText(site.getAirDays() + " ngày");
        lblDetailOtherInfo.setText(site.getOtherInfo() != null && !site.getOtherInfo().trim().isEmpty() ? site.getOtherInfo() : "(Không có thông tin khác)");

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
        btnEditSite.setDisable(false);
        btnToggleActive.setDisable(false);

        try {
            tblOrderHistory.setItems(FXCollections.observableArrayList(orderService.getOrdersBySite(site.getSiteCode())));
        } catch (Exception e) {
            tblOrderHistory.setItems(FXCollections.observableArrayList());
        }
    }

    private void clearDetails() {
        lblDetailSiteCode.setText(""); lblDetailName.setText(""); lblDetailShipDays.setText("");
        lblDetailAirDays.setText(""); lblDetailStatus.setText(""); lblDetailOtherInfo.setText("");
        btnEditSite.setDisable(true); btnToggleActive.setDisable(true);
        btnToggleActive.setText("🚫 Ngừng Hoạt Động"); btnToggleActive.setStyle("");
        tblOrderHistory.setItems(FXCollections.observableArrayList());
    }

    @FXML
    private void handleRefresh() {
        Site selected = tblSites.getSelectionModel().getSelectedItem();
        loadSitesData();
        if (selected != null) {
            tblSites.getItems().stream()
                .filter(s -> s.getSiteCode().equals(selected.getSiteCode()))
                .findFirst()
                .ifPresent(s -> tblSites.getSelectionModel().select(s));
        } else clearDetails();
    }

    @FXML private void handleCreateSite() {
        SiteDialogHelper.openSiteDialog(tblSites, null, this::loadSitesData);
        clearDetails();
    }

    @FXML private void handleEditSite() {
        Site selected = tblSites.getSelectionModel().getSelectedItem();
        if (selected != null) SiteDialogHelper.openSiteDialog(tblSites, selected, this::handleRefresh);
    }

    @FXML
    private void handleToggleActive() {
        Site selected = tblSites.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        String action = selected.isActive() ? "ngừng hoạt động" : "tái kích hoạt";
        if (AlertHelper.showConfirm("Xác nhận", "Bạn có chắc chắn muốn " + action + " đối tác Site '" + selected.getName() + "' không?")) {
            try {
                siteService.toggleSiteActiveStatus(selected.getSiteCode());
                AlertHelper.showInfo("Thành công", "Cập nhật trạng thái thành công!");
                loadSitesData();
                tblSites.getItems().stream()
                    .filter(s -> s.getSiteCode().equals(selected.getSiteCode()))
                    .findFirst()
                    .ifPresent(s -> tblSites.getSelectionModel().select(s));
            } catch (Exception e) {
                AlertHelper.showError("Lỗi", e.getMessage());
            }
        }
    }
}
