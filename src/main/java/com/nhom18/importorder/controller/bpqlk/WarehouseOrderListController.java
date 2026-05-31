package com.nhom18.importorder.controller.bpqlk;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.enums.OrderStatus;
import com.nhom18.importorder.service.WarehouseService;
import com.nhom18.importorder.util.AlertHelper;
import com.nhom18.importorder.util.NavigationManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class WarehouseOrderListController {

    @FXML
    private TableView<Order> tblOrders;
    @FXML
    private TableColumn<Order, Integer> colOrderId;
    @FXML
    private TableColumn<Order, Integer> colReqId;
    @FXML
    private TableColumn<Order, String> colSiteName;
    @FXML
    private TableColumn<Order, String> colDeliveryMethod;
    @FXML
    private TableColumn<Order, String> colEstArrival;
    @FXML
    private TableColumn<Order, String> colStatus;

    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cbStatusFilter;
    @FXML
    private Button btnInspect;

    private final WarehouseService warehouseService;
    private List<Order> allOrders = new ArrayList<>();

    public WarehouseOrderListController() {
        this.warehouseService = new WarehouseService();
    }

    @FXML
    public void initialize() {
        // 1. Ánh xạ các cột
        colOrderId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        colReqId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getRequestId()).asObject());
        colSiteName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSiteName()));
        colDeliveryMethod.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDeliveryMethod().name()));
        colEstArrival.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstimatedArrival().toString()));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));

        // Tự động định dạng màu sắc cho cột trạng thái
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    switch (OrderStatus.valueOf(item)) {
                        case PENDING -> {
                            setText("Chờ nhập kho (PENDING)");
                            setStyle("-fx-text-fill: #fb923c; -fx-font-weight: bold;");
                        }
                        case CONFIRMED -> {
                            setText("Chờ nhập kho (CONFIRMED)");
                            setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                        }
                        case SHIPPED -> {
                            setText("Chờ nhập kho (SHIPPED)");
                            setStyle("-fx-text-fill: #06b6d4; -fx-font-weight: bold;");
                        }
                        case DELIVERED -> {
                            setText("Đã nhập kho (DELIVERED)");
                            setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                        }
                        case CANCELLED -> {
                            setText("Đã hủy (CANCELLED)");
                            setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                        }
                    }
                }
            }
        });

        // 2. Thiết lập Combobox Bộ lọc trạng thái
        cbStatusFilter.setItems(FXCollections.observableArrayList(
            "Tất cả", "Chờ nhập kho", "Đã nhập kho", "Đã hủy"
        ));
        cbStatusFilter.setValue("Chờ nhập kho"); // Mặc định hiển thị đơn chờ nhập kho

        // Lắng nghe sự kiện đổi bộ lọc hoặc gõ tìm kiếm
        cbStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterAndSearchOrders());
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterAndSearchOrders());

        // Lắng nghe chọn dòng để kích hoạt nút Kiểm hàng
        btnInspect.disableProperty().bind(tblOrders.getSelectionModel().selectedItemProperty().isNull());

        // Hỗ trợ đúp chuột để xem chi tiết đối chiếu
        tblOrders.setRowFactory(tv -> {
            TableRow<Order> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    handleInspectOrder();
                }
            });
            return row;
        });

        // Tải dữ liệu ban đầu
        loadData();
    }

    private void loadData() {
        allOrders = warehouseService.getAllOrders();
        filterAndSearchOrders();
    }

    private void filterAndSearchOrders() {
        if (allOrders == null) return;

        String selectedFilter = cbStatusFilter.getValue();
        String searchKeyword = txtSearch.getText().trim().toLowerCase();

        List<Order> filtered = allOrders.stream()
            // 1. Lọc theo trạng thái nhập kho
            .filter(o -> {
                if ("Chờ nhập kho".equals(selectedFilter)) {
                    return o.getStatus() == OrderStatus.PENDING || 
                           o.getStatus() == OrderStatus.CONFIRMED || 
                           o.getStatus() == OrderStatus.SHIPPED;
                } else if ("Đã nhập kho".equals(selectedFilter)) {
                    return o.getStatus() == OrderStatus.DELIVERED;
                } else if ("Đã hủy".equals(selectedFilter)) {
                    return o.getStatus() == OrderStatus.CANCELLED;
                }
                return true; // Tất cả
            })
            // 2. Lọc theo từ khóa tìm kiếm (Mã đơn, Tên site, hoặc Mã sản phẩm)
            .filter(o -> {
                if (searchKeyword.isEmpty()) return true;
                
                boolean matchOrderId = String.valueOf(o.getId()).contains(searchKeyword);
                boolean matchSiteName = o.getSiteName().toLowerCase().contains(searchKeyword);
                
                // Kiểm tra xem có chứa mã sản phẩm nào khớp trong đơn hàng không
                boolean matchProductCode = o.getItems().stream()
                    .anyMatch(item -> item.getMerchandiseCode().toLowerCase().contains(searchKeyword));
                
                return matchOrderId || matchSiteName || matchProductCode;
            })
            .collect(Collectors.toList());

        tblOrders.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleInspectOrder() {
        Order selected = tblOrders.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Chưa chọn đơn hàng", "Vui lòng chọn đơn hàng chờ nhập kho trong danh sách!");
            return;
        }

        if (selected.getStatus() == OrderStatus.DELIVERED) {
            AlertHelper.showWarning("Đơn hàng đã nhập kho", "Đơn hàng này đã được xác nhận nhập kho trước đó!");
            return;
        }

        if (selected.getStatus() == OrderStatus.CANCELLED) {
            AlertHelper.showWarning("Đơn hàng đã hủy", "Đơn hàng này đã bị hủy, không thể tiến hành kiểm nhận kho!");
            return;
        }

        // Truyền tham số ID đơn sang Controller chi tiết
        WarehouseReceiptConfirmController.selectedOrderId = selected.getId();
        // Điều hướng sang màn hình đối chiếu kiểm nhận
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpqlk/warehouse_receipt_confirm.fxml");
    }
}
