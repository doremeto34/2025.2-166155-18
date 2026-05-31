package com.nhom18.importorder.controller.bpbh;

import com.nhom18.importorder.model.entity.Merchandise;
import com.nhom18.importorder.service.MerchandiseService;
import com.nhom18.importorder.util.AlertHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class MerchandiseListController {

    @FXML
    private TableView<Merchandise> tblMerchandise;
    @FXML
    private TableColumn<Merchandise, String> colCode;
    @FXML
    private TableColumn<Merchandise, String> colName;
    @FXML
    private TableColumn<Merchandise, String> colDescription;
    @FXML
    private TableColumn<Merchandise, String> colUnit;
    @FXML
    private TableColumn<Merchandise, Double> colPrice;
    @FXML
    private TableColumn<Merchandise, String> colStatus;

    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cbStatusFilter;
    @FXML
    private Label lblTotalItems;

    /* Chi tiết mặt hàng */
    @FXML
    private Label lblDetailCode;
    @FXML
    private Label lblDetailName;
    @FXML
    private Label lblDetailUnit;
    @FXML
    private Label lblDetailPrice;
    @FXML
    private Label lblDetailStatus;
    @FXML
    private Label lblDetailDescription;

    @FXML
    private Button btnEditMerchandise;
    @FXML
    private Button btnToggleActive;

    private final MerchandiseService merchandiseService;
    private List<Merchandise> allMerchandiseList;

    public MerchandiseListController() {
        this.merchandiseService = new MerchandiseService();
    }

    @FXML
    public void initialize() {
        // 1. Ánh xạ các cột TableColumn với thuộc tính Entity
        colCode.setCellValueFactory(new PropertyValueFactory<>("merchandiseCode"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isActive() ? "Đang kinh doanh" : "Ngừng kinh doanh"));

        // Định dạng cột Giá thành tiền tệ
        colPrice.setCellFactory(column -> new TableCell<Merchandise, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("$%,.2f", price));
                }
            }
        });

        // Định dạng cột trạng thái đẹp mắt
        colStatus.setCellFactory(column -> new TableCell<Merchandise, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("Đang kinh doanh")) {
                        setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;"); // Xanh
                    } else {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;"); // Đỏ
                    }
                }
            }
        });

        // 2. Khởi tạo Combobox Lọc Trạng Thái
        cbStatusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Đang kinh doanh", "Ngừng kinh doanh"));
        cbStatusFilter.setValue("Tất cả");

        // Lắng nghe sự kiện đổi bộ lọc hoặc tìm kiếm động
        cbStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterMerchandise());
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterMerchandise());

        // Lắng nghe chọn dòng trong bảng để hiển thị chi tiết
        tblMerchandise.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            displayMerchandiseDetails(newSelection);
        });

        // 3. Tải dữ liệu ban đầu
        loadMerchandiseData();
        clearDetails();
    }

    private void loadMerchandiseData() {
        try {
            allMerchandiseList = merchandiseService.getAllMerchandise();
            filterMerchandise();
        } catch (Exception e) {
            AlertHelper.showError("Lỗi nạp dữ liệu", "Không thể tải danh sách mặt hàng:\n" + e.getMessage());
        }
    }

    private void filterMerchandise() {
        if (allMerchandiseList == null) return;

        String keyword = txtSearch.getText() != null ? txtSearch.getText().trim() : "";
        String statusFilter = cbStatusFilter.getValue();

        List<Merchandise> filtered = merchandiseService.searchAllMerchandise(keyword, statusFilter);
        tblMerchandise.setItems(FXCollections.observableArrayList(filtered));
        updateItemCount(filtered.size());
    }

    private void updateItemCount(int count) {
        lblTotalItems.setText(String.format("Tổng số: %d mặt hàng", count));
    }

    private void displayMerchandiseDetails(Merchandise m) {
        if (m == null) {
            clearDetails();
            return;
        }

        lblDetailCode.setText(m.getMerchandiseCode());
        lblDetailName.setText(m.getName());
        lblDetailUnit.setText(m.getUnit());
        lblDetailPrice.setText(String.format("$%,.2f", m.getPrice()));
        lblDetailDescription.setText(m.getDescription() != null && !m.getDescription().trim().isEmpty()
            ? m.getDescription() : "(Không có mô tả sản phẩm)");

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
        lblDetailCode.setText("");
        lblDetailName.setText("");
        lblDetailUnit.setText("");
        lblDetailPrice.setText("");
        lblDetailStatus.setText("");
        lblDetailDescription.setText("");

        btnEditMerchandise.setDisable(true);
        btnToggleActive.setDisable(true);
        btnToggleActive.setText("🚫 Ngừng Kinh Doanh");
        btnToggleActive.setStyle("");
    }

    @FXML
    private void handleRefresh() {
        Merchandise selected = tblMerchandise.getSelectionModel().getSelectedItem();
        loadMerchandiseData();
        if (selected != null) {
            for (Merchandise m : tblMerchandise.getItems()) {
                if (m.getMerchandiseCode().equals(selected.getMerchandiseCode())) {
                    tblMerchandise.getSelectionModel().select(m);
                    break;
                }
            }
        } else {
            clearDetails();
        }
    }

    @FXML
    private void handleCreateMerchandise() {
        openMerchandiseDialog(null);
    }

    @FXML
    private void handleEditMerchandise() {
        Merchandise selected = tblMerchandise.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openMerchandiseDialog(selected);
        }
    }

    private void openMerchandiseDialog(Merchandise m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom18/importorder/view/bpbh/merchandise_dialog.fxml"));
            Parent root = loader.load();

            MerchandiseDialogController controller = loader.getController();
            controller.setMerchandise(m);

            Stage stage = new Stage();
            stage.setTitle(m == null ? "Thêm Mặt Hàng Mới" : "Cập Nhật Thông Tin Mặt Hàng");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(tblMerchandise.getScene().getWindow());

            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            } catch (Exception e) {
                // Bỏ qua nếu lỗi CSS
            }

            stage.setScene(scene);
            controller.setStage(stage);
            stage.showAndWait();

            if (controller.isSaved()) {
                loadMerchandiseData();
                if (m != null) {
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
        Merchandise selected = tblMerchandise.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String action = selected.isActive() ? "ngừng kinh doanh" : "kích hoạt lại";
        boolean confirm = AlertHelper.showConfirm("Xác nhận thay đổi trạng thái",
            "Bạn có chắc chắn muốn " + action + " mặt hàng '" + selected.getName() + "' không?");

        if (confirm) {
            try {
                merchandiseService.toggleMerchandiseActiveStatus(selected.getMerchandiseCode());
                AlertHelper.showInfo("Thành công", "Đã cập nhật trạng thái kinh doanh của mặt hàng thành công!");
                loadMerchandiseData();
                // Chọn lại dòng để xem chi tiết mới nhất
                for (Merchandise m : tblMerchandise.getItems()) {
                    if (m.getMerchandiseCode().equals(selected.getMerchandiseCode())) {
                        tblMerchandise.getSelectionModel().select(m);
                        break;
                    }
                }
            } catch (IllegalStateException e) {
                AlertHelper.showError("Ràng buộc nghiệp vụ", e.getMessage());
            } catch (IllegalArgumentException e) {
                AlertHelper.showError("Lỗi nghiệp vụ", e.getMessage());
            } catch (Exception e) {
                AlertHelper.showError("Lỗi", "Không thể cập nhật trạng thái kinh doanh:\n" + e.getMessage());
            }
        }
    }
}
