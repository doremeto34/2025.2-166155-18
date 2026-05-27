package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.enums.OrderStatus;
import com.nhom18.importorder.service.OrderService;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class OrderListController {

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
    private TableColumn<Order, String> colCreatedDate;
    @FXML
    private TableColumn<Order, String> colEstArrival;
    @FXML
    private TableColumn<Order, String> colStatus;

    // --- Cột chi tiết đơn hàng ---
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
    private javafx.scene.control.Button btnReallocate;

    private final OrderService orderService;
    private List<Order> allOrdersList;

    public OrderListController() {
        this.orderService = new OrderService();
    }

    @FXML
    public void initialize() {
        // 1. Cấu hình các cột của TableView đơn hàng
        colOrderId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        colReqId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getRequestId()).asObject());
        colSiteName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSiteName()));
        colDeliveryMethod.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDeliveryMethod().name()));
        colCreatedDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedDate().toString()));
        colEstArrival.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstimatedArrival().toString()));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));

        // Tự động tô màu cột trạng thái để giao diện sinh động và dễ nhìn
        colStatus.setCellFactory(column -> new TableCell<>() {
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

        // 2. Cấu hình các cột của TableView chi tiết đơn hàng
        colItemCode.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchandiseCode()));
        colItemName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchandiseName()));
        colItemQtyOrdered.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantityOrdered()).asObject());
        colItemQtyConfirmed.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantityConfirmed()).asObject());
        colItemQtyReceived.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantityReceived()).asObject());
        colItemUnit.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUnit()));

        // 3. Thiết lập Combobox Bộ lọc trạng thái
        cbOrderStatusFilter.setItems(FXCollections.observableArrayList(
            "Tất cả", "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"
        ));
        cbOrderStatusFilter.setValue("Tất cả");

        // Lắng nghe sự kiện đổi bộ lọc
        cbOrderStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterOrders(newVal));

        // Lắng nghe chọn dòng trong TableView để hiển thị chi tiết và kích hoạt nút Tái phân bổ
        tblOrders.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                tblOrderItems.setItems(FXCollections.observableArrayList(newSelection.getItems()));
                
                if (newSelection.getStatus() == OrderStatus.CANCELLED 
                        && (newSelection.getCancelReason() == null || !newSelection.getCancelReason().contains("[REALLOCATED]"))) {
                    btnReallocate.setDisable(false);
                } else {
                    btnReallocate.setDisable(true);
                }
            } else {
                tblOrderItems.setItems(FXCollections.observableArrayList());
                btnReallocate.setDisable(true);
            }
        });

        // Tải dữ liệu ban đầu
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
            List<Order> filtered = allOrdersList.stream()
                .filter(o -> o.getStatus().name().equalsIgnoreCase(status))
                .collect(Collectors.toList());
            tblOrders.setItems(FXCollections.observableArrayList(filtered));
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
            com.nhom18.importorder.util.AlertHelper.showError("Lỗi", "Vui lòng chọn một đơn hàng bị hủy để tái phân bổ.");
            return;
        }

        if (selectedOrder.getStatus() != OrderStatus.CANCELLED) {
            com.nhom18.importorder.util.AlertHelper.showError("Lỗi", "Chỉ có thể tái phân bổ đơn hàng có trạng thái BỊ HỦY (CANCELLED)!");
            return;
        }

        boolean confirm = com.nhom18.importorder.util.AlertHelper.showConfirm("Xác nhận", 
            "Bạn có chắc muốn chạy lại thuật toán phân bổ để tìm Site thay thế cho đơn hàng #" + selectedOrder.getId() + " không?");
        if (!confirm) {
            return;
        }

        try {
            orderService.reallocateCancelledOrder(selectedOrder.getId());
            com.nhom18.importorder.util.AlertHelper.showInfo("Thành công", 
                "Tái phân bổ thành công! Đơn hàng thay thế đã được tạo ở trạng thái PENDING.");
            loadOrdersData();
        } catch (Exception e) {
            com.nhom18.importorder.util.AlertHelper.showError("Lỗi Tái Phân Bổ", 
                "Không thể tái phân bổ tự động:\n" + e.getMessage());
        }
    }
}
