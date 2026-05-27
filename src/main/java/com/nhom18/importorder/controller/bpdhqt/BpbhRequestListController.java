package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.dao.IImportRequestDAO;
import com.nhom18.importorder.dao.impl.SQLiteImportRequestDAO;
import com.nhom18.importorder.model.entity.ImportRequest;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.enums.RequestStatus;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class BpbhRequestListController {

    @FXML
    private ComboBox<String> cbStatusFilter;

    @FXML
    private TableView<ImportRequest> tblRequests;
    @FXML
    private TableColumn<ImportRequest, Integer> colId;
    @FXML
    private TableColumn<ImportRequest, String> colCreator;
    @FXML
    private TableColumn<ImportRequest, String> colDate;
    @FXML
    private TableColumn<ImportRequest, RequestStatus> colStatus;

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

    private final IImportRequestDAO requestDAO = new SQLiteImportRequestDAO();
    private final ObservableList<ImportRequest> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. Ánh xạ cột của bảng Yêu cầu
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCreator.setCellValueFactory(new PropertyValueFactory<>("creatorName"));
        colDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedDate().toString()));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Custom badge hiển thị trạng thái
        colStatus.setCellFactory(column -> new TableCell<ImportRequest, RequestStatus>() {
            @Override
            protected void updateItem(RequestStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label();
                    badge.getStyleClass().add("badge");
                    
                    switch (item) {
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
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        // 2. Ánh xạ cột của bảng mặt hàng chi tiết
        colItemMerch.setCellValueFactory(new PropertyValueFactory<>("merchandiseCode"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("merchandiseName"));
        colItemQty.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantityOrdered()).asObject());
        colItemUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colItemDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDesiredDeliveryDate().toString()));

        // Lắng nghe sự kiện chọn yêu cầu để hiển thị chi tiết mặt hàng
        tblRequests.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadRequestItems(newVal.getId());
            } else {
                tblRequestItems.getItems().clear();
            }
        });

        // 3. Thiết lập ComboBox lọc trạng thái
        cbStatusFilter.setItems(FXCollections.observableArrayList(
            "Tất cả trạng thái", 
            "Chờ xử lý (PENDING)", 
            "Đang xử lý (PROCESSING)", 
            "Đã phê duyệt (APPROVED)", 
            "Bị từ chối (REJECTED)"
        ));
        cbStatusFilter.setValue("Tất cả trạng thái");
        cbStatusFilter.valueProperty().addListener((observable, oldValue, newValue) -> filterRequests(newValue));

        // Tải dữ liệu ban đầu
        loadRequests();
    }

    private void loadRequests() {
        List<ImportRequest> requests = requestDAO.getAllWithCreatorName();
        masterData.setAll(requests);
        tblRequests.setItems(masterData);
    }

    private void loadRequestItems(int requestId) {
        ImportRequest fullRequest = requestDAO.getById(requestId);
        if (fullRequest != null) {
            tblRequestItems.setItems(FXCollections.observableArrayList(fullRequest.getItems()));
        } else {
            tblRequestItems.getItems().clear();
        }
    }

    private void filterRequests(String filterOption) {
        if (filterOption == null || filterOption.equals("Tất cả trạng thái")) {
            tblRequests.setItems(masterData);
            return;
        }

        RequestStatus targetStatus = switch (filterOption) {
            case "Chờ xử lý (PENDING)" -> RequestStatus.PENDING;
            case "Đang xử lý (PROCESSING)" -> RequestStatus.PROCESSING;
            case "Đã phê duyệt (APPROVED)" -> RequestStatus.APPROVED;
            case "Bị từ chối (REJECTED)" -> RequestStatus.REJECTED;
            default -> null;
        };

        if (targetStatus == null) {
            tblRequests.setItems(masterData);
        } else {
            List<ImportRequest> filtered = masterData.stream()
                .filter(r -> r.getStatus() == targetStatus)
                .collect(Collectors.toList());
            tblRequests.setItems(FXCollections.observableArrayList(filtered));
        }
    }

    @FXML
    private void handleRefresh() {
        loadRequests();
        cbStatusFilter.setValue("Tất cả trạng thái");
        tblRequestItems.getItems().clear();
    }
}
