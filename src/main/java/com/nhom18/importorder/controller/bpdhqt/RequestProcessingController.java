package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.dao.IImportRequestDAO;
import com.nhom18.importorder.dao.impl.SQLiteImportRequestDAO;
import com.nhom18.importorder.model.entity.ImportRequest;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.enums.RequestStatus;
import com.nhom18.importorder.service.OrderService;
import com.nhom18.importorder.util.AlertHelper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class RequestProcessingController {

    // Lớp DTO phụ trợ để hiển thị danh sách dòng phân bổ đề xuất ở bảng bên phải
    public static class ProposedAllocationRow {
        private final String merchandiseCode;
        private final String merchandiseName;
        private final String siteName;
        private final String deliveryMethod;
        private final int quantity;
        private final LocalDate arrivalDate;

        public ProposedAllocationRow(String merchandiseCode, String merchandiseName, String siteName, String deliveryMethod, int quantity, LocalDate arrivalDate) {
            this.merchandiseCode = merchandiseCode;
            this.merchandiseName = merchandiseName;
            this.siteName = siteName;
            this.deliveryMethod = deliveryMethod;
            this.quantity = quantity;
            this.arrivalDate = arrivalDate;
        }

        public String getMerchandiseCode() { return merchandiseCode; }
        public String getMerchandiseName() { return merchandiseName; }
        public String getSiteName() { return siteName; }
        public String getDeliveryMethod() { return deliveryMethod; }
        public int getQuantity() { return quantity; }
        public LocalDate getArrivalDate() { return arrivalDate; }
    }

    @FXML
    private TabPane tabPaneRequest;
    @FXML
    private Tab tabSelect;
    @FXML
    private Tab tabProposed;

    @FXML
    private TableView<ImportRequest> tblPendingRequests;
    @FXML
    private TableColumn<ImportRequest, Integer> colReqId;
    @FXML
    private TableColumn<ImportRequest, String> colReqCreator;
    @FXML
    private TableColumn<ImportRequest, String> colReqDate;

    @FXML
    private TableView<ImportRequestItem> tblRequestItems;
    @FXML
    private TableColumn<ImportRequestItem, String> colItemMerch;
    @FXML
    private TableColumn<ImportRequestItem, String> colItemName;
    @FXML
    private TableColumn<ImportRequestItem, Integer> colItemQty;
    @FXML
    private TableColumn<ImportRequestItem, String> colItemUnit;
    @FXML
    private TableColumn<ImportRequestItem, String> colItemDate;

    @FXML
    private TableView<ProposedAllocationRow> tblAllocationProposed;
    @FXML
    private TableColumn<ProposedAllocationRow, String> colPropMerch;
    @FXML
    private TableColumn<ProposedAllocationRow, String> colPropName;
    @FXML
    private TableColumn<ProposedAllocationRow, String> colPropSite;
    @FXML
    private TableColumn<ProposedAllocationRow, String> colPropMethod;
    @FXML
    private TableColumn<ProposedAllocationRow, Integer> colPropQty;
    @FXML
    private TableColumn<ProposedAllocationRow, String> colPropArrival;

    @FXML
    private Button btnRunAllocation;
    @FXML
    private Button btnCancelProposed;
    @FXML
    private Button btnApproveOrders;
    
    @FXML
    private VBox boxError;
    @FXML
    private Label lblAllocationError;
    @FXML
    private Label lblProposedPlaceholder;

    private final IImportRequestDAO requestDAO;
    private final OrderService orderService;
    
    // Lưu tạm danh sách các đơn hàng đề xuất được sinh ra sau khi chạy thuật toán
    private List<Order> currentProposedOrders = new ArrayList<>();

    public RequestProcessingController() {
        this.requestDAO = new SQLiteImportRequestDAO();
        this.orderService = new OrderService();
    }

    @FXML
    public void initialize() {
        // 1. Cấu hình bảng yêu cầu PENDING bên trái
        colReqId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        colReqCreator.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatorName()));
        colReqDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedDate().toString()));

        // 2. Cấu hình bảng chi tiết mặt hàng được chọn
        colItemMerch.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchandiseCode()));
        colItemName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchandiseName()));
        colItemQty.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantityOrdered()).asObject());
        colItemUnit.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUnit()));
        colItemDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDesiredDeliveryDate().toString()));

        // 3. Cấu hình bảng đề xuất phân bổ bên phải
        colPropMerch.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchandiseCode()));
        colPropName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchandiseName()));
        colPropSite.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSiteName()));
        colPropMethod.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDeliveryMethod()));
        colPropQty.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantity()).asObject());
        colPropArrival.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getArrivalDate().toString()));

        // Lắng nghe sự kiện chọn yêu cầu để cập nhật bảng mặt hàng chi tiết
        tblPendingRequests.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                loadRequestItems(newSelection);
                resetProposedAllocationPane();
            } else {
                tblRequestItems.getItems().clear();
                resetProposedAllocationPane();
            }
        });

        // Thiết lập trạng thái ban đầu cho các nút hành động bên phải
        btnCancelProposed.setDisable(true);
        btnApproveOrders.setDisable(true);

        // Nạp danh sách yêu cầu PENDING
        loadPendingRequests();
    }

    private void loadPendingRequests() {
        List<ImportRequest> pendingRequests = requestDAO.getAllWithCreatorName().stream()
            .filter(r -> r.getStatus() == RequestStatus.PENDING)
            .collect(Collectors.toList());
        tblPendingRequests.setItems(FXCollections.observableArrayList(pendingRequests));
    }

    private void loadRequestItems(ImportRequest request) {
        ImportRequest fullRequest = requestDAO.getById(request.getId());
        if (fullRequest != null) {
            tblRequestItems.setItems(FXCollections.observableArrayList(fullRequest.getItems()));
        } else {
            tblRequestItems.getItems().clear();
        }
    }

    private void resetProposedAllocationPane() {
        tblAllocationProposed.getItems().clear();
        currentProposedOrders.clear();
        
        boxError.setVisible(false);
        boxError.setManaged(false);
        
        btnCancelProposed.setDisable(true);
        btnApproveOrders.setDisable(true);
        
        lblProposedPlaceholder.setText("Chưa có dữ liệu phân bổ. Vui lòng bấm 'Chạy Phân Bổ Tự Động' ở cột bên trái.");
    }

    @FXML
    private void handleRunAllocation() {
        ImportRequest selectedRequest = tblPendingRequests.getSelectionModel().getSelectedItem();
        if (selectedRequest == null) {
            AlertHelper.showWarning("Chưa chọn yêu cầu", "Vui lòng chọn một phiếu yêu cầu trong danh sách để chạy phân bổ!");
            return;
        }

        try {
            // Chạy thuật toán phân bổ site tự động
            List<Order> proposed = orderService.generateProposedOrders(selectedRequest.getId());
            currentProposedOrders = proposed;

            // Chuyển cấu trúc gom nhóm sang dạng dòng phẳng để hiển thị trên TableView
            List<ProposedAllocationRow> rows = new ArrayList<>();
            for (Order order : proposed) {
                for (OrderItem item : order.getItems()) {
                    rows.add(new ProposedAllocationRow(
                        item.getMerchandiseCode(),
                        item.getMerchandiseName(),
                        order.getSiteName(),
                        order.getDeliveryMethod().name(),
                        item.getQuantityOrdered(),
                        order.getEstimatedArrival()
                    ));
                }
            }

            tblAllocationProposed.setItems(FXCollections.observableArrayList(rows));
            
            // Ẩn panel thông báo lỗi
            boxError.setVisible(false);
            boxError.setManaged(false);

            // Kích hoạt các nút hành động
            btnCancelProposed.setDisable(false);
            btnApproveOrders.setDisable(false);

            // Chuyển sang Tab 2 xem đề xuất
            tabPaneRequest.getSelectionModel().select(tabProposed);

        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            // Đã có lỗi xảy ra (Ví dụ: thiếu tồn kho, không kịp thời gian giao)
            lblAllocationError.setText(e.getMessage());
            boxError.setVisible(true);
            boxError.setManaged(true);

            tblAllocationProposed.getItems().clear();
            currentProposedOrders.clear();

            btnCancelProposed.setDisable(false); // Cho phép quay lại tab 1
            btnApproveOrders.setDisable(true);

            lblProposedPlaceholder.setText("Lỗi phân bổ: Không thể tìm thấy phương án đáp ứng nhu cầu.");

            // Chuyển sang Tab 2 xem lỗi
            tabPaneRequest.getSelectionModel().select(tabProposed);
        }
    }

    @FXML
    private void handleCancelProposed() {
        resetProposedAllocationPane();
        tabPaneRequest.getSelectionModel().select(tabSelect);
    }

    @FXML
    private void handleApproveOrders() {
        ImportRequest selectedRequest = tblPendingRequests.getSelectionModel().getSelectedItem();
        if (selectedRequest == null || currentProposedOrders.isEmpty()) {
            return;
        }

        boolean confirm = AlertHelper.showConfirm("Xác nhận phê duyệt", 
            "Hệ thống sẽ tiến hành tạo " + currentProposedOrders.size() + " đơn đặt hàng và trừ tồn kho dự phòng tương ứng của các Site đối tác.\nBạn có đồng ý?");
        
        if (confirm) {
            try {
                // Lưu đơn hàng & trừ tồn kho
                orderService.confirmAndSaveOrders(selectedRequest.getId(), currentProposedOrders);
                
                AlertHelper.showInfo("Duyệt thành công", 
                    "Phiếu yêu cầu #" + selectedRequest.getId() + " đã được xử lý thành công!\n" +
                    "Đã sinh ra " + currentProposedOrders.size() + " đơn đặt hàng quốc tế.");

                // Reset giao diện và nạp lại danh sách yêu cầu
                resetProposedAllocationPane();
                tblRequestItems.getItems().clear();
                loadPendingRequests();

                // Quay lại bước 1
                tabPaneRequest.getSelectionModel().select(tabSelect);

            } catch (Exception e) {
                AlertHelper.showError("Lỗi hệ thống", "Đã xảy ra lỗi trong quá trình tạo đơn hàng: " + e.getMessage());
            }
        }
    }
}
