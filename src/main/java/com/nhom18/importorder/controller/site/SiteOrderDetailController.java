package com.nhom18.importorder.controller.site;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.enums.OrderStatus;
import com.nhom18.importorder.service.OrderService;
import com.nhom18.importorder.util.AlertHelper;
import com.nhom18.importorder.util.NavigationManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SiteOrderDetailController {

    public static int selectedOrderId;

    @FXML private Label lblTitle, lblOrderId, lblDeliveryMethod, lblCreatedDate, lblEstArrival, lblStatus, lblCancelReasonTitle, lblCancelReason;
    @FXML private TableView<OrderItem> tblOrderItems;
    @FXML private TableColumn<OrderItem, String> colItemCode, colItemName, colUnit;
    @FXML private TableColumn<OrderItem, Integer> colQtyOrdered;
    @FXML private Button btnReject, btnConfirm, btnShip;

    private final OrderService orderService = new OrderService();

    @FXML
    public void initialize() {
        colItemCode.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchandiseCode()));
        colItemName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchandiseName()));
        colQtyOrdered.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantityOrdered()).asObject());
        colUnit.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUnit()));
        loadOrderDetail();
    }

    private void loadOrderDetail() {
        if (selectedOrderId <= 0) { AlertHelper.showError("Lỗi", "Mã đơn hàng không hợp lệ."); handleBack(); return; }
        Order order = orderService.getOrderById(selectedOrderId);
        if (order == null) { AlertHelper.showError("Lỗi", "Đơn hàng không tồn tại."); handleBack(); return; }

        lblTitle.setText("📦 CHI TIẾT ĐƠN HÀNG NHẬN ĐƯỢC #" + order.getId());
        lblOrderId.setText(String.valueOf(order.getId()));
        lblDeliveryMethod.setText(order.getDeliveryMethod().name());
        lblCreatedDate.setText(order.getCreatedDate().toString());
        lblEstArrival.setText(order.getEstimatedArrival().toString());
        lblStatus.setText(order.getStatus().name());

        switch (order.getStatus()) {
            case PENDING -> lblStatus.setStyle("-fx-text-fill: #fb923c; -fx-font-weight: bold;");
            case CONFIRMED -> lblStatus.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
            case SHIPPED -> lblStatus.setStyle("-fx-text-fill: #06b6d4; -fx-font-weight: bold;");
            case DELIVERED -> lblStatus.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
            case CANCELLED -> lblStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        }

        boolean isCancelled = order.getStatus() == OrderStatus.CANCELLED;
        lblCancelReasonTitle.setVisible(isCancelled); lblCancelReason.setVisible(isCancelled);
        if (isCancelled) lblCancelReason.setText(order.getCancelReason() != null ? order.getCancelReason() : "Không có lý do");

        tblOrderItems.setItems(FXCollections.observableArrayList(order.getItems()));

        boolean isPending = order.getStatus() == OrderStatus.PENDING;
        boolean isConfirmed = order.getStatus() == OrderStatus.CONFIRMED;
        btnConfirm.setDisable(!isPending);
        btnReject.setDisable(!isPending && !isConfirmed);
        btnShip.setDisable(!isConfirmed);
    }

    @FXML private void handleBack() { NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/site/site_order_list.fxml"); }

    @FXML
    private void handleConfirm() {
        if (AlertHelper.showConfirm("Xác nhận", "Bạn có chắc muốn XÁC NHẬN đơn hàng này không?")) {
            try {
                orderService.updateOrderShipmentStatus(selectedOrderId, OrderStatus.CONFIRMED);
                AlertHelper.showInfo("Thành công", "Đã xác nhận đơn hàng!"); loadOrderDetail();
            } catch (Exception e) { AlertHelper.showError("Lỗi", e.getMessage()); }
        }
    }

    @FXML
    private void handleShip() {
        if (AlertHelper.showConfirm("Xác nhận xuất hàng", "Bạn có chắc muốn xuất hàng đi (SHIPPED) không?")) {
            try {
                orderService.updateOrderShipmentStatus(selectedOrderId, OrderStatus.SHIPPED);
                AlertHelper.showInfo("Thành công", "Đơn hàng đã được xuất vận chuyển!"); loadOrderDetail();
            } catch (Exception e) { AlertHelper.showError("Lỗi", e.getMessage()); }
        }
    }

    @FXML
    private void handleReject() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Từ chối đơn hàng");
        dialog.setHeaderText("Lý do từ chối đơn hàng bắt buộc");
        dialog.setContentText("Nhập lý do:");
        try { dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm()); } catch (Exception ignored) {}
        
        dialog.showAndWait().ifPresent(val -> {
            String reason = val.trim();
            if (reason.isEmpty()) AlertHelper.showWarning("Cảnh báo", "Bạn phải nhập lý do từ chối!");
            else {
                try {
                    orderService.handleCancelledOrder(selectedOrderId, reason);
                    AlertHelper.showInfo("Thành công", "Đã từ chối đơn hàng."); loadOrderDetail();
                } catch (Exception e) { AlertHelper.showError("Lỗi", e.getMessage()); }
            }
        });
    }
}
