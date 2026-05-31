package com.nhom18.importorder.controller.bpqlk;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.enums.OrderStatus;
import com.nhom18.importorder.service.WarehouseService;
import com.nhom18.importorder.util.AlertHelper;
import com.nhom18.importorder.util.NavigationManager;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class WarehouseOrderListController {

    @FXML private TableView<Order> tblOrders;
    @FXML private TableColumn<Order, Integer> colOrderId, colReqId;
    @FXML private TableColumn<Order, String> colSiteName, colDeliveryMethod, colEstArrival, colStatus;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbStatusFilter;
    @FXML private Button btnInspect;

    private final WarehouseService warehouseService = new WarehouseService();
    private List<Order> allOrders = new ArrayList<>();

    @FXML
    public void initialize() {
        WarehouseOrderTableHelper.setupColumns(colOrderId, colReqId, colSiteName, colDeliveryMethod, colEstArrival, colStatus);
        
        cbStatusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Chờ nhập kho", "Đã nhập kho", "Đã hủy"));
        cbStatusFilter.setValue("Chờ nhập kho");

        cbStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterAndSearchOrders());
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterAndSearchOrders());
        btnInspect.disableProperty().bind(tblOrders.getSelectionModel().selectedItemProperty().isNull());

        tblOrders.setRowFactory(tv -> {
            TableRow<Order> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) handleInspectOrder();
            });
            return row;
        });

        loadData();
    }

    private void loadData() {
        allOrders = warehouseService.getAllOrders();
        filterAndSearchOrders();
    }

    private void filterAndSearchOrders() {
        if (allOrders == null) return;
        List<Order> filtered = WarehouseOrderTableHelper.filter(allOrders, cbStatusFilter.getValue(), txtSearch.getText());
        tblOrders.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML private void handleRefresh() { loadData(); }

    @FXML
    private void handleInspectOrder() {
        Order selected = tblOrders.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Chưa chọn đơn hàng", "Vui lòng chọn đơn hàng chờ nhập kho!"); return;
        }
        if (selected.getStatus() == OrderStatus.DELIVERED) {
            AlertHelper.showWarning("Đơn hàng đã nhập kho", "Đơn hàng này đã được nhập kho!"); return;
        }
        if (selected.getStatus() == OrderStatus.CANCELLED) {
            AlertHelper.showWarning("Đơn hàng đã hủy", "Đơn hàng đã bị hủy, không thể nhập kho!"); return;
        }

        WarehouseReceiptConfirmController.selectedOrderId = selected.getId();
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpqlk/warehouse_receipt_confirm.fxml");
    }
}
