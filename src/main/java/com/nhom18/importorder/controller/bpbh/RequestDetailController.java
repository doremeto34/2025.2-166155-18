package com.nhom18.importorder.controller.bpbh;

import com.nhom18.importorder.model.entity.ImportRequest;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.enums.RequestStatus;
import com.nhom18.importorder.service.ImportRequestService;
import com.nhom18.importorder.util.NavigationManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;

public class RequestDetailController {

    // Trường static được gán từ màn hình danh sách trước khi chuyển hướng
    public static int selectedRequestId = -1;

    @FXML
    private Label lblRequestId;

    @FXML
    private Label lblCreatorName;

    @FXML
    private Label lblCreatedDate;

    @FXML
    private StackPane paneStatusBadge;

    @FXML
    private TableView<ImportRequestItem> tblRequestItems;

    @FXML
    private TableColumn<ImportRequestItem, String> colMerchandiseCode;

    @FXML
    private TableColumn<ImportRequestItem, String> colMerchandiseName;

    @FXML
    private TableColumn<ImportRequestItem, Integer> colQuantity;

    @FXML
    private TableColumn<ImportRequestItem, String> colUnit;

    @FXML
    private TableColumn<ImportRequestItem, LocalDate> colDesiredDate;

    private final ImportRequestService requestService = new ImportRequestService();

    @FXML
    public void initialize() {
        // 1. Ánh xạ các cột TableView
        colMerchandiseCode.setCellValueFactory(new PropertyValueFactory<>("merchandiseCode"));
        colMerchandiseName.setCellValueFactory(new PropertyValueFactory<>("merchandiseName"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantityOrdered"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colDesiredDate.setCellValueFactory(new PropertyValueFactory<>("desiredDeliveryDate"));

        // 2. Tải chi tiết yêu cầu
        if (selectedRequestId != -1) {
            loadRequestDetail(selectedRequestId);
        } else {
            lblRequestId.setText("N/A");
        }
    }

    private void loadRequestDetail(int requestId) {
        ImportRequest req = requestService.getRequestById(requestId);
        if (req != null) {
            lblRequestId.setText("#" + req.getId());
            lblCreatorName.setText(req.getCreatorName());
            lblCreatedDate.setText(req.getCreatedDate().toString());
            
            // Render Badge Trạng Thái
            Label badge = new Label();
            badge.getStyleClass().add("badge");
            
            switch (req.getStatus()) {
                case PENDING -> {
                    badge.setText("Chờ Xử Lý");
                    badge.getStyleClass().add("badge-pending");
                }
                case PROCESSING -> {
                    badge.setText("Đang Xử Lý");
                    badge.getStyleClass().add("badge-processing");
                }
                case APPROVED -> {
                    badge.setText("Đã Phê Duyệt");
                    badge.getStyleClass().add("badge-approved");
                }
                case REJECTED -> {
                    badge.setText("Bị Từ Chối");
                    badge.getStyleClass().add("badge-rejected");
                }
            }
            paneStatusBadge.getChildren().setAll(badge);

            // Gán danh sách các mặt hàng cho TableView
            tblRequestItems.setItems(FXCollections.observableArrayList(req.getItems()));
        } else {
            lblRequestId.setText("Không tìm thấy!");
        }
    }

    @FXML
    private void handleBack() {
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpbh/request_list.fxml");
    }
}
