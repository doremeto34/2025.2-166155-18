package com.nhom18.importorder.controller.bpqlk;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.service.OrderService;
import com.nhom18.importorder.service.WarehouseService;
import com.nhom18.importorder.util.AlertHelper;
import com.nhom18.importorder.util.NavigationManager;
import com.nhom18.importorder.util.SessionManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class WarehouseReceiptConfirmController {

    public static int selectedOrderId = -1;

    // DTO phục vụ đối chiếu hàng hóa trực tiếp trên TableView
    public static class ReconciliationRow {
        private final int itemId;
        private final String merchCode;
        private final String merchName;
        private final int qtyOrdered;
        private final int qtyConfirmed;
        private final String unit;
        
        private final SimpleBooleanProperty checked = new SimpleBooleanProperty(false);
        private final SimpleIntegerProperty qtyReceived = new SimpleIntegerProperty(0);
        private final SimpleIntegerProperty discrepancy = new SimpleIntegerProperty(0);
        private final SimpleStringProperty notes = new SimpleStringProperty("");

        public ReconciliationRow(int itemId, String merchCode, String merchName, int qtyOrdered, int qtyConfirmed, String unit) {
            this.itemId = itemId;
            this.merchCode = merchCode;
            this.merchName = merchName;
            this.qtyOrdered = qtyOrdered;
            this.qtyConfirmed = qtyConfirmed;
            this.unit = unit;
            
            // Mặc định gán số thực nhận bằng số site xác nhận giao (để giảm công nhập liệu)
            setQtyReceived(qtyConfirmed);
        }

        public int getItemId() { return itemId; }
        public String getMerchCode() { return merchCode; }
        public String getMerchName() { return merchName; }
        public int getQtyOrdered() { return qtyOrdered; }
        public int getQtyConfirmed() { return qtyConfirmed; }
        public String getUnit() { return unit; }

        public SimpleBooleanProperty checkedProperty() { return checked; }
        public boolean isChecked() { return checked.get(); }
        public void setChecked(boolean checked) { this.checked.set(checked); }

        public SimpleIntegerProperty qtyReceivedProperty() { return qtyReceived; }
        public int getQtyReceived() { return qtyReceived.get(); }
        public void setQtyReceived(int qtyReceived) { 
            this.qtyReceived.set(qtyReceived);
            this.discrepancy.set(qtyReceived - qtyConfirmed);
        }

        public SimpleIntegerProperty discrepancyProperty() { return discrepancy; }
        public int getDiscrepancy() { return discrepancy.get(); }

        public SimpleStringProperty notesProperty() { return notes; }
        public String getNotes() { return notes.get(); }
        public void setNotes(String notes) { this.notes.set(notes); }
    }

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
        // 1. Ánh xạ các cột tĩnh
        colMerchCode.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchCode()));
        colMerchName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchName()));
        colQtyOrdered.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQtyOrdered()).asObject());
        colQtyConfirmed.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQtyConfirmed()).asObject());
        colDiscrepancy.setCellValueFactory(cell -> cell.getValue().discrepancyProperty().asObject());

        // 2. Cột Checkbox đã kiểm tra (tương tác trực tiếp)
        colChecked.setCellValueFactory(cell -> cell.getValue().checkedProperty());
        colChecked.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    ReconciliationRow row = getTableRow().getItem();
                    if (row != null) {
                        row.setChecked(checkBox.isSelected());
                    }
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ReconciliationRow row = getTableRow().getItem();
                    if (row != null) {
                        checkBox.setSelected(row.isChecked());
                    }
                    setGraphic(checkBox);
                }
            }
        });

        // 3. Cột số lượng thực nhận (Hỗ trợ TextBox thay đổi số lượng thực tế nhận được)
        colQtyReceived.setCellValueFactory(cell -> cell.getValue().qtyReceivedProperty().asObject());
        colQtyReceived.setCellFactory(column -> new TableCell<>() {
            private final TextField txtQty = new TextField();
            private boolean isUpdating = false;
            {
                txtQty.setPrefWidth(80);
                txtQty.setStyle("-fx-alignment: CENTER;");
                txtQty.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (isUpdating) return;
                    ReconciliationRow row = getTableRow().getItem();
                    if (row != null && newVal != null && !newVal.trim().isEmpty()) {
                        try {
                            int val = Integer.parseInt(newVal.trim());
                            if (val >= 0) {
                                row.setQtyReceived(val);
                            }
                        } catch (NumberFormatException e) {
                            // Bỏ qua giá trị nhập không hợp lệ
                        }
                    }
                });
            }
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ReconciliationRow row = getTableRow().getItem();
                    if (row != null) {
                        isUpdating = true;
                        txtQty.setText(String.valueOf(row.getQtyReceived()));
                        isUpdating = false;
                    }
                    setGraphic(txtQty);
                }
            }
        });

        // 4. Định dạng màu sắc cột chênh lệch
        colDiscrepancy.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item > 0 ? "+" + item : String.valueOf(item));
                    if (item < 0) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); // Đỏ (Hụt hàng)
                    } else if (item > 0) {
                        setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;"); // Xanh lá (Dư hàng)
                    } else {
                        setStyle("-fx-text-fill: -text-secondary;"); // 0 (Đủ hàng)
                    }
                }
            }
        });

        // 5. Cột ghi chú lý do sai lệch (Nhập văn bản trực tiếp)
        colDiscrepancyNotes.setCellValueFactory(cell -> cell.getValue().notesProperty());
        colDiscrepancyNotes.setCellFactory(column -> new TableCell<>() {
            private final TextField txtNote = new TextField();
            private boolean isUpdating = false;
            {
                txtNote.setPromptText("Nhập lý do nếu lệch...");
                txtNote.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (isUpdating) return;
                    ReconciliationRow row = getTableRow().getItem();
                    if (row != null) {
                        row.setNotes(newVal);
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ReconciliationRow row = getTableRow().getItem();
                    if (row != null) {
                        isUpdating = true;
                        txtNote.setText(row.getNotes() != null ? row.getNotes() : "");
                        isUpdating = false;
                    }
                    setGraphic(txtNote);
                }
            }
        });

        // Tải chi tiết đơn hàng được chọn
        if (selectedOrderId != -1) {
            loadOrderDetail(selectedOrderId);
        }
    }

    private void loadOrderDetail(int orderId) {
        // Tải thông tin từ OrderService để có đủ liên kết các dòng hàng
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

            // Chuyển danh mục chi tiết sang dòng đối chiếu
            List<ReconciliationRow> rows = new ArrayList<>();
            for (OrderItem item : currentOrder.getItems()) {
                // Đối với site_confirmed_quantity, nếu site chưa cập nhật (mặc định = 0), ta lấy số quantity_ordered để làm mốc đối chiếu
                int confirmedQty = item.getQuantityConfirmed();
                if (confirmedQty <= 0) {
                    confirmedQty = item.getQuantityOrdered();
                    item.setQuantityConfirmed(confirmedQty); // Cập nhật tạm
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
        
        // Tiền điều kiện 1: Phải tích chọn kiểm hàng cho TẤT CẢ các sản phẩm
        boolean allChecked = rows.stream().allMatch(ReconciliationRow::isChecked);
        if (!allChecked) {
            AlertHelper.showWarning("Kiểm hàng chưa hoàn tất", "Bạn cần kiểm tra thực tế và tích chọn 'Đã kiểm' cho TẤT CẢ sản phẩm trong danh sách trước khi xác nhận nhập kho!");
            return;
        }

        // Tiền điều kiện 2: Kiểm tra chênh lệch và bắt buộc nhập ghi chú
        for (ReconciliationRow row : rows) {
            if (row.getDiscrepancy() != 0 && (row.getNotes() == null || row.getNotes().trim().isEmpty())) {
                AlertHelper.showWarning("Thiếu lý do sai lệch", 
                    "Mặt hàng '" + row.getMerchName() + "' bị chênh lệch số lượng (" + (row.getDiscrepancy() > 0 ? "+" : "") + row.getDiscrepancy() + ").\nVui lòng nhập lý do chênh lệch vào cột 'Ghi chú sai lệch'!");
                return;
            }
        }

        // Tiền điều kiện 3: Số lượng thực nhận không được âm
        boolean negativeQty = rows.stream().anyMatch(row -> row.getQtyReceived() < 0);
        if (negativeQty) {
            AlertHelper.showError("Số lượng không hợp lệ", "Số lượng thực tế nhập kho của mặt hàng không thể nhỏ hơn 0!");
            return;
        }

        boolean confirm = AlertHelper.showConfirm("Xác nhận nhập kho", 
            "Hệ thống sẽ ghi nhận phiếu nhập kho của đơn hàng #" + currentOrder.getId() + " và đồng bộ dữ liệu số lượng thực tế vào CSDL.\nBạn đồng ý?");
        
        if (confirm) {
            try {
                // Lấy thông tin tài khoản đăng nhập hiện tại
                int currentUserId = 8; // Mặc định ID tài khoản thai_bpqlk là 8 trong schema
                if (SessionManager.getInstance().getCurrentUser() != null) {
                    currentUserId = SessionManager.getInstance().getCurrentUser().getId();
                }

                // Chuyển đổi dữ liệu dòng đối chiếu ngược lại thực thể OrderItem
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
                    
                    // Nối thêm ghi chú sai lệch vào ghi chú chi tiết nếu có
                    itemsToUpdate.add(item);
                }

                String notes = txtReceiptNotes.getText().trim();
                
                // Đồng bộ và xác nhận qua WarehouseService
                warehouseService.confirmReceipt(currentOrder.getId(), currentUserId, itemsToUpdate, notes);
                
                AlertHelper.showInfo("Xác nhận nhập kho thành công", 
                    "Đơn hàng #" + currentOrder.getId() + " đã được kiểm nhận nhập kho hoàn tất!\n" +
                    "Hồ sơ phiếu nhập kho đã được ghi nhận và đồng bộ thành công.");
                
                // Quay lại danh sách đơn hàng nhập kho
                handleBack();

            } catch (Exception e) {
                AlertHelper.showError("Lỗi hệ thống", "Đã xảy ra lỗi trong quá trình xác nhận nhập kho: " + e.getMessage());
            }
        }
    }
}
