package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.enums.OrderStatus;
import com.nhom18.importorder.service.OrderService;
import com.nhom18.importorder.util.AlertHelper;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class OrderListController {

    @FXML
    private TableView<Order> tblOrders;
    @FXML
    private TableColumn<Order, Integer> colOrderId;
    @FXML
    private TableColumn<Order, String> colSiteName;
    @FXML
    private TableColumn<Order, String> colDeliveryMethod;
    @FXML
    private TableColumn<Order, String> colCreatedDate;
    @FXML
    private TableColumn<Order, String> colEstArrival;
    @FXML
    private TableColumn<Order, String> colStatus;

    @FXML
    private TableView<OrderItem> tblOrderItems;
    @FXML
    private TableColumn<OrderItem, String> colItemCode;
    @FXML
    private TableColumn<OrderItem, String> colItemName;
    @FXML
    private TableColumn<OrderItem, Integer> colItemQtyOrdered;
    @FXML
    private TableColumn<OrderItem, Integer> colItemQtyConfirmed;
    @FXML
    private TableColumn<OrderItem, Integer> colItemQtyReceived;
    @FXML
    private TableColumn<OrderItem, String> colItemUnit;

    @FXML
    private ComboBox<String> cbOrderStatusFilter;
    @FXML
    private Button btnReallocate;

    private final OrderService orderService;
    private List<Order> allOrdersList;

    public OrderListController() {
        this.orderService = new OrderService();
    }

    @FXML
    public void initialize() {
        OrderListTableHelper.setupOrderColumns(
            colOrderId, colSiteName, colDeliveryMethod, colCreatedDate, colEstArrival, colStatus
        );
        OrderListTableHelper.setupItemColumns(
            colItemCode, colItemName, colItemQtyOrdered, colItemQtyConfirmed, colItemQtyReceived, colItemUnit
        );

        cbOrderStatusFilter.setItems(FXCollections.observableArrayList(
            "Tất cả", "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"
        ));
        cbOrderStatusFilter.setValue("Tất cả");
        cbOrderStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterOrders(newVal));

        tblOrders.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                tblOrderItems.setItems(FXCollections.observableArrayList(newSelection.getItems()));
                btnReallocate.setDisable(newSelection.getStatus() != OrderStatus.CANCELLED 
                    || (newSelection.getCancelReason() != null && newSelection.getCancelReason().contains("[REALLOCATED]")));
            } else {
                tblOrderItems.setItems(FXCollections.observableArrayList());
                btnReallocate.setDisable(true);
            }
        });

        loadOrdersData();
    }

    private void loadOrdersData() {
        allOrdersList = orderService.getAllOrders();
        filterOrders(cbOrderStatusFilter.getValue());
    }

    private void filterOrders(String status) {
        if (allOrdersList == null) return;
        if (status == null || status.equals("Tất cả")) {
            tblOrders.setItems(FXCollections.observableArrayList(allOrdersList));
        } else {
            tblOrders.setItems(FXCollections.observableArrayList(
                allOrdersList.stream()
                    .filter(o -> o.getStatus().name().equalsIgnoreCase(status))
                    .collect(Collectors.toList())
            ));
        }
    }

    @FXML
    private void handleRefresh() {
        loadOrdersData();
    }

    @FXML
    private void handleReallocate() {
        Order selectedOrder = tblOrders.getSelectionModel().getSelectedItem();
        if (selectedOrder == null) {
            AlertHelper.showError("Lỗi", "Vui lòng chọn một đơn hàng bị hủy để tái phân bổ.");
            return;
        }
        if (selectedOrder.getStatus() != OrderStatus.CANCELLED) {
            AlertHelper.showError("Lỗi", "Chỉ có thể tái phân bổ đơn hàng có trạng thái BỊ HỦY (CANCELLED)!");
            return;
        }

        if (AlertHelper.showConfirm("Xác nhận", 
            "Bạn có chắc muốn chạy lại thuật toán phân bổ để tìm Site thay thế cho đơn hàng #" + selectedOrder.getId() + " không?")) {
            try {
                orderService.reallocateCancelledOrder(selectedOrder.getId());
                AlertHelper.showInfo("Thành công", "Tái phân bổ thành công! Đơn hàng thay thế đã được tạo ở trạng thái PENDING.");
                loadOrdersData();
            } catch (Exception e) {
                AlertHelper.showError("Lỗi Tái Phân Bổ", "Không thể tái phân bổ tự động:\n" + e.getMessage());
            }
        }
    }
}
