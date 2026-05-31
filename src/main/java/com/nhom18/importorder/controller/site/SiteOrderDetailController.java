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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;

public class SiteOrderDetailController {

    public static int selectedOrderId;

    @FXML
    private Label lblTitle;
    @FXML
    private Label lblOrderId;
    @FXML
    private Label lblDeliveryMethod;
    @FXML
    private Label lblCreatedDate;
    @FXML
    private Label lblEstArrival;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblCancelReasonTitle;
    @FXML
    private Label lblCancelReason;

    @FXML
    private TableView<OrderItem> tblOrderItems;
    @FXML
    private TableColumn<OrderItem, String> colItemCode;
    @FXML
    private TableColumn<OrderItem, String> colItemName;
    @FXML
    private TableColumn<OrderItem, Integer> colQtyOrdered;
    @FXML
    private TableColumn<OrderItem, String> colUnit;

    @FXML
    private Button btnReject;
    @FXML
    private Button btnConfirm;
    @FXML
    private Button btnShip;

    private final OrderService orderService;

    public SiteOrderDetailController() {
        this.orderService = new OrderService();
    }

    @FXML
    public void initialize() {
        // 1. Ánh xạ các cột trong bảng mặt hàng
        colItemCode.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchandiseCode()));
        colItemName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchandiseName()));
        colQtyOrdered.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantityOrdered()).asObject());
        colUnit.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUnit()));

        loadOrderDetail();
    }

    private void loadOrderDetail() {
        if (selectedOrderId <= 0) {
            AlertHelper.showError("Lỗi", "Không tìm thấy mã đơn hàng được chọn.");
            handleBack();
            return;
        }

        Order order = orderService.getOrderById(selectedOrderId);
        if (order == null) {
            AlertHelper.showError("Lỗi", "Đơn hàng không tồn tại trên hệ thống.");
            handleBack();
            return;
        }

        // 2. Cập nhật các Label hiển thị
        lblTitle.setText("📦 CHI TIẾT ĐƠN HÀNG NHẬN ĐƯỢC #" + order.getId());
        lblOrderId.setText(String.valueOf(order.getId()));
        lblDeliveryMethod.setText(order.getDeliveryMethod().name());
        lblCreatedDate.setText(order.getCreatedDate().toString());
        lblEstArrival.setText(order.getEstimatedArrival().toString());
        lblStatus.setText(order.getStatus().name());

        // Tô màu cho Label Trạng thái
        switch (order.getStatus()) {
            case PENDING -> lblStatus.setStyle("-fx-text-fill: #fb923c; -fx-font-weight: bold;");
            case CONFIRMED -> lblStatus.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
            case SHIPPED -> lblStatus.setStyle("-fx-text-fill: #06b6d4; -fx-font-weight: bold;");
            case DELIVERED -> lblStatus.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
            case CANCELLED -> lblStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        }

        // Hiển thị lý do từ chối nếu đơn bị hủy
        if (order.getStatus() == OrderStatus.CANCELLED) {
            lblCancelReasonTitle.setVisible(true);
            lblCancelReason.setVisible(true);
            lblCancelReason.setText(order.getCancelReason() != null ? order.getCancelReason() : "Không có lý do");
        } else {
            lblCancelReasonTitle.setVisible(false);
            lblCancelReason.setVisible(false);
        }

        // 3. Đổ dữ liệu vào bảng
        tblOrderItems.setItems(FXCollections.observableArrayList(order.getItems()));

        // 4. Quản lý trạng thái các nút bấm hành động phê duyệt
        if (order.getStatus() == OrderStatus.PENDING) {
            btnConfirm.setDisable(false);
            btnReject.setDisable(false);
            btnShip.setDisable(true);
        } else if (order.getStatus() == OrderStatus.CONFIRMED) {
            btnConfirm.setDisable(true);
            btnReject.setDisable(false);
            btnShip.setDisable(false);
        } else {
            // SHIPPED, DELIVERED, CANCELLED đều không được sửa đổi
            btnConfirm.setDisable(true);
            btnReject.setDisable(true);
            btnShip.setDisable(true);
        }
    }

    @FXML
    private void handleBack() {
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/site/site_order_list.fxml");
    }

    @FXML
    private void handleConfirm() {
        boolean confirm = AlertHelper.showConfirm("Xác nhận đơn hàng", 
            "Bạn có chắc muốn XÁC NHẬN cung cấp đơn hàng này và chuẩn bị đóng hàng không?");
        if (confirm) {
            try {
                orderService.updateOrderShipmentStatus(selectedOrderId, OrderStatus.CONFIRMED);
                AlertHelper.showInfo("Thành công", "Đã xác nhận đơn hàng thành công!");
                loadOrderDetail();
            } catch (Exception e) {
                AlertHelper.showError("Lỗi", "Không thể xác nhận đơn hàng:\n" + e.getMessage());
            }
        }
    }

    @FXML
    private void handleShip() {
        boolean confirm = AlertHelper.showConfirm("Xác nhận xuất hàng", 
            "Bạn có chắc chắn đã XUẤT HÀNG đi và muốn cập nhật trạng thái đơn thành SHIPPED không?");
        if (confirm) {
            try {
                orderService.updateOrderShipmentStatus(selectedOrderId, OrderStatus.SHIPPED);
                AlertHelper.showInfo("Thành công", "Đơn hàng đã được chuyển trạng thái sang ĐANG VẬN CHUYỂN (SHIPPED)!");
                loadOrderDetail();
            } catch (Exception e) {
                AlertHelper.showError("Lỗi", "Không thể cập nhật trạng thái đơn hàng:\n" + e.getMessage());
            }
        }
    }

    @FXML
    private void handleReject() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Từ chối đơn hàng");
        dialog.setHeaderText("Lý do từ chối đơn hàng bắt buộc");
        dialog.setContentText("Nhập lý do từ chối:");
        
        // Style dialog to look beautiful
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            dialog.getDialogPane().getStyleClass().add("card");
        } catch (Exception e) {
            // ignore
        }

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String reason = result.get().trim();
            if (reason.isEmpty()) {
                AlertHelper.showWarning("Cảnh báo", "Bạn phải nhập lý do từ chối đơn hàng!");
                return;
            }

            try {
                orderService.handleCancelledOrder(selectedOrderId, reason);
                AlertHelper.showInfo("Thành công", "Đã từ chối cung cấp đơn hàng. Tồn kho ảo đã được giải phóng!");
                loadOrderDetail();
            } catch (Exception e) {
                AlertHelper.showError("Lỗi", "Không thể hủy đơn hàng:\n" + e.getMessage());
            }
        }
    }
}
