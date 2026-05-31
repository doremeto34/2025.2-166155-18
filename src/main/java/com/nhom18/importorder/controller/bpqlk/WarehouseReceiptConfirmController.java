package com.nhom18.importorder.controller.bpqlk;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.service.OrderService;
import com.nhom18.importorder.service.WarehouseService;
import com.nhom18.importorder.util.AlertHelper;
import com.nhom18.importorder.util.NavigationManager;
import com.nhom18.importorder.util.SessionManager;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

public class WarehouseReceiptConfirmController {

    public static int selectedOrderId = -1;

    @FXML
    private Label lblOrderSubtitle;
    @FXML
    private Label lblOrderId;
    @FXML
    private Label lblReqId;
    @FXML
    private Label lblSiteName;
    @FXML
    private Label lblDeliveryMethod;
    @FXML
    private TextArea txtReceiptNotes;

    @FXML
    private TableView<ReconciliationRow> tblReconciliation;
    @FXML
    private TableColumn<ReconciliationRow, Boolean> colChecked;
    @FXML
    private TableColumn<ReconciliationRow, String> colMerchCode;
    @FXML
    private TableColumn<ReconciliationRow, String> colMerchName;
    @FXML
    private TableColumn<ReconciliationRow, Integer> colQtyOrdered;
    @FXML
    private TableColumn<ReconciliationRow, Integer> colQtyConfirmed;
    @FXML
    private TableColumn<ReconciliationRow, Integer> colQtyReceived;
    @FXML
    private TableColumn<ReconciliationRow, Integer> colDiscrepancy;
    @FXML
    private TableColumn<ReconciliationRow, String> colDiscrepancyNotes;

    @FXML
    private Button btnConfirmReceipt;

    private final WarehouseService warehouseService;
    private final OrderService orderService;
    private Order currentOrder;

    public WarehouseReceiptConfirmController() {
        this.warehouseService = new WarehouseService();
        this.orderService = new OrderService();
    }

    @FXML
    public void initialize() {
        WarehouseReceiptTableHelper.setupColumns(
            colChecked, colMerchCode, colMerchName, colQtyOrdered,
            colQtyConfirmed, colQtyReceived, colDiscrepancy, colDiscrepancyNotes
        );

        if (selectedOrderId != -1) {
            loadOrderDetail(selectedOrderId);
        }
    }

    private void loadOrderDetail(int orderId) {
        List<Order> list = orderService.getAllOrders();
        currentOrder = list.stream()
            .filter(o -> o.getId() == orderId)
            .findFirst()
            .orElse(null);

        if (currentOrder != null) {
            lblOrderSubtitle.setText("Đơn đặt hàng #" + currentOrder.getId() + " | Site đối tác: " + currentOrder.getSiteName());
            lblOrderId.setText(String.valueOf(currentOrder.getId()));
            lblReqId.setText("#" + currentOrder.getRequestId());
            lblSiteName.setText(currentOrder.getSiteName() + " (" + currentOrder.getSiteCode() + ")");
            lblDeliveryMethod.setText(currentOrder.getDeliveryMethod().name() + " (Ngày về dự kiến: " + currentOrder.getEstimatedArrival() + ")");

            List<ReconciliationRow> rows = new ArrayList<>();
            for (OrderItem item : currentOrder.getItems()) {
                int confirmedQty = item.getQuantityConfirmed();
                if (confirmedQty <= 0) {
                    confirmedQty = item.getQuantityOrdered();
                    item.setQuantityConfirmed(confirmedQty);
                }
                
                rows.add(new ReconciliationRow(
                    item.getId(),
                    item.getMerchandiseCode(),
                    item.getMerchandiseName(),
                    item.getQuantityOrdered(),
                    confirmedQty,
                    item.getUnit()
                ));
            }
            tblReconciliation.setItems(FXCollections.observableArrayList(rows));
        } else {
            AlertHelper.showError("Không tìm thấy đơn hàng", "Đơn hàng này không tồn tại trong cơ sở dữ liệu!");
            handleBack();
        }
    }

    @FXML
    private void handleBack() {
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpqlk/warehouse_order_list.fxml");
    }

    @FXML
    private void handleConfirmReceipt() {
        if (currentOrder == null) return;
        List<ReconciliationRow> rows = tblReconciliation.getItems();
        
        if (!rows.stream().allMatch(ReconciliationRow::isChecked)) {
            AlertHelper.showWarning("Kiểm hàng chưa hoàn tất", "Bạn cần kiểm tra thực tế và tích chọn 'Đã kiểm' cho TẤT CẢ sản phẩm trong danh sách trước khi xác nhận nhập kho!");
            return;
        }

        for (ReconciliationRow row : rows) {
            if (row.getDiscrepancy() != 0 && (row.getNotes() == null || row.getNotes().trim().isEmpty())) {
                AlertHelper.showWarning("Thiếu lý do sai lệch", 
                    "Mặt hàng '" + row.getMerchName() + "' bị chênh lệch số lượng (" + (row.getDiscrepancy() > 0 ? "+" : "") + row.getDiscrepancy() + ").\nVui lòng nhập lý do chênh lệch vào cột 'Ghi chú sai lệch'!");
                return;
            }
        }

        if (rows.stream().anyMatch(row -> row.getQtyReceived() < 0)) {
            AlertHelper.showError("Số lượng không hợp lệ", "Số lượng thực tế nhập kho của mặt hàng không thể nhỏ hơn 0!");
            return;
        }

        if (AlertHelper.showConfirm("Xác nhận nhập kho", 
            "Hệ thống sẽ ghi nhận phiếu nhập kho của đơn hàng #" + currentOrder.getId() + " và đồng bộ dữ liệu số lượng thực tế vào CSDL.\nBạn đồng ý?")) {
            try {
                int currentUserId = 8;
                if (SessionManager.getInstance().getCurrentUser() != null) {
                    currentUserId = SessionManager.getInstance().getCurrentUser().getId();
                }

                List<OrderItem> itemsToUpdate = new ArrayList<>();
                for (ReconciliationRow row : rows) {
                    OrderItem item = new OrderItem();
                    item.setId(row.getItemId());
                    item.setOrderId(currentOrder.getId());
                    item.setMerchandiseCode(row.getMerchCode());
                    item.setQuantityOrdered(row.getQtyOrdered());
                    item.setQuantityConfirmed(row.getQtyConfirmed());
                    item.setQuantityReceived(row.getQtyReceived());
                    item.setUnit(row.getUnit());
                    itemsToUpdate.add(item);
                }

                warehouseService.confirmReceipt(currentOrder.getId(), currentUserId, itemsToUpdate, txtReceiptNotes.getText().trim());
                AlertHelper.showInfo("Xác nhận nhập kho thành công", "Đơn hàng #" + currentOrder.getId() + " đã được kiểm nhận nhập kho hoàn tất!");
                handleBack();
            } catch (Exception e) {
                AlertHelper.showError("Lỗi hệ thống", "Đã xảy ra lỗi trong quá trình xác nhận nhập kho: " + e.getMessage());
            }
        }
    }
}
