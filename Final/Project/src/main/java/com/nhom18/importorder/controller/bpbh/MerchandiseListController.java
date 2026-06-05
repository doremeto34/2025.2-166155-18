package com.nhom18.importorder.controller.bpbh;

import com.nhom18.importorder.model.entity.Merchandise;
import com.nhom18.importorder.service.MerchandiseService;
import com.nhom18.importorder.util.AlertHelper;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class MerchandiseListController {

    @FXML private TableView<Merchandise> tblMerchandise;
    @FXML private TableColumn<Merchandise, String> colCode, colName, colDescription, colUnit, colStatus;
    @FXML private TableColumn<Merchandise, Double> colPrice;

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbStatusFilter;
    @FXML private Label lblTotalItems;

    @FXML private Label lblDetailCode, lblDetailName, lblDetailUnit, lblDetailPrice, lblDetailStatus, lblDetailDescription;
    @FXML private Button btnEditMerchandise, btnToggleActive;

    private final MerchandiseService merchandiseService = new MerchandiseService();
    private List<Merchandise> allMerchandiseList;

    @FXML
    public void initialize() {
        MerchandiseTableHelper.setupColumns(colCode, colName, colDescription, colUnit, colPrice, colStatus);
        
        cbStatusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Đang kinh doanh", "Ngừng kinh doanh"));
        cbStatusFilter.setValue("Tất cả");

        cbStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterMerchandise());
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterMerchandise());
        tblMerchandise.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> displayMerchandiseDetails(newSel));

        loadMerchandiseData();
        clearDetails();
    }

    private void loadMerchandiseData() {
        try {
            allMerchandiseList = merchandiseService.getAllMerchandise();
            filterMerchandise();
        } catch (Exception e) {
            AlertHelper.showError("Lỗi", "Không tải được danh sách: " + e.getMessage());
        }
    }

    private void filterMerchandise() {
        if (allMerchandiseList == null) return;
        List<Merchandise> filtered = merchandiseService.searchAllMerchandise(txtSearch.getText(), cbStatusFilter.getValue());
        tblMerchandise.setItems(FXCollections.observableArrayList(filtered));
        lblTotalItems.setText(String.format("Tổng số: %d mặt hàng", filtered.size()));
    }

    private void displayMerchandiseDetails(Merchandise m) {
        if (m == null) { clearDetails(); return; }
        lblDetailCode.setText(m.getMerchandiseCode());
        lblDetailName.setText(m.getName());
        lblDetailUnit.setText(m.getUnit());
        lblDetailPrice.setText(String.format("$%,.2f", m.getPrice()));
        lblDetailDescription.setText(m.getDescription() != null && !m.getDescription().trim().isEmpty() ? m.getDescription() : "(Không có mô tả sản phẩm)");

        if (m.isActive()) {
            lblDetailStatus.setText("ĐANG KINH DOANH");
            lblDetailStatus.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
            btnToggleActive.setText("🚫 Ngừng Kinh Doanh");
            btnToggleActive.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;");
        } else {
            lblDetailStatus.setText("NGỪNG KINH DOANH");
            lblDetailStatus.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            btnToggleActive.setText("✅ Kích Hoạt Lại");
            btnToggleActive.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold;");
        }
        btnEditMerchandise.setDisable(false);
        btnToggleActive.setDisable(false);
    }

    private void clearDetails() {
        lblDetailCode.setText(""); lblDetailName.setText(""); lblDetailUnit.setText("");
        lblDetailPrice.setText(""); lblDetailStatus.setText(""); lblDetailDescription.setText("");
        btnEditMerchandise.setDisable(true); btnToggleActive.setDisable(true);
        btnToggleActive.setText("🚫 Ngừng Kinh Doanh"); btnToggleActive.setStyle("");
    }

    @FXML
    private void handleRefresh() {
        Merchandise selected = tblMerchandise.getSelectionModel().getSelectedItem();
        loadMerchandiseData();
        if (selected != null) {
            tblMerchandise.getItems().stream()
                .filter(m -> m.getMerchandiseCode().equals(selected.getMerchandiseCode()))
                .findFirst()
                .ifPresent(m -> tblMerchandise.getSelectionModel().select(m));
        } else clearDetails();
    }

    @FXML private void handleCreateMerchandise() {
        MerchandiseDialogHelper.openMerchandiseDialog(tblMerchandise, null, this::loadMerchandiseData);
        clearDetails();
    }

    @FXML private void handleEditMerchandise() {
        Merchandise selected = tblMerchandise.getSelectionModel().getSelectedItem();
        if (selected != null) MerchandiseDialogHelper.openMerchandiseDialog(tblMerchandise, selected, this::handleRefresh);
    }

    @FXML
    private void handleToggleActive() {
        Merchandise selected = tblMerchandise.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        String action = selected.isActive() ? "ngừng kinh doanh" : "kích hoạt lại";
        if (AlertHelper.showConfirm("Xác nhận", "Bạn có chắc muốn " + action + " mặt hàng '" + selected.getName() + "' không?")) {
            try {
                merchandiseService.toggleMerchandiseActiveStatus(selected.getMerchandiseCode());
                AlertHelper.showInfo("Thành công", "Cập nhật trạng thái thành công!");
                loadMerchandiseData();
                tblMerchandise.getItems().stream()
                    .filter(m -> m.getMerchandiseCode().equals(selected.getMerchandiseCode()))
                    .findFirst()
                    .ifPresent(m -> tblMerchandise.getSelectionModel().select(m));
            } catch (Exception e) {
                AlertHelper.showError("Lỗi", e.getMessage());
            }
        }
    }
}
