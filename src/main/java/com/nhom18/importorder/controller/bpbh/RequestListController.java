package com.nhom18.importorder.controller.bpbh;

import com.nhom18.importorder.model.entity.ImportRequest;
import com.nhom18.importorder.model.enums.RequestStatus;
import com.nhom18.importorder.service.ImportRequestService;
import com.nhom18.importorder.util.NavigationManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class RequestListController {

    @FXML
    private ComboBox<String> cbStatusFilter;

    @FXML
    private TableView<ImportRequest> tblRequests;

    @FXML
    private TableColumn<ImportRequest, Integer> colId;

    @FXML
    private TableColumn<ImportRequest, String> colCreator;

    @FXML
    private TableColumn<ImportRequest, LocalDate> colDate;

    @FXML
    private TableColumn<ImportRequest, RequestStatus> colStatus;

    @FXML
    private TableColumn<ImportRequest, Void> colAction;

    private final ImportRequestService requestService = new ImportRequestService();
    private final ObservableList<ImportRequest> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. Ánh xạ dữ liệu cột
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCreator.setCellValueFactory(new PropertyValueFactory<>("creatorName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // 2. Custom hiển thị trạng thái dạng Pill Badge
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

        // 3. Nút hành động "Xem chi tiết" cho từng hàng
        colAction.setCellFactory(new Callback<TableColumn<ImportRequest, Void>, TableCell<ImportRequest, Void>>() {
            @Override
            public TableCell<ImportRequest, Void> call(final TableColumn<ImportRequest, Void> param) {
                return new TableCell<ImportRequest, Void>() {
                    private final Button btnDetail = new Button("👁 Chi tiết");

                    {
                        btnDetail.getStyleClass().addAll("button-secondary");
                        btnDetail.setStyle("-fx-padding: 4px 10px; -fx-font-size: 12px;");
                        btnDetail.setOnAction(event -> {
                            ImportRequest req = getTableView().getItems().get(getIndex());
                            openRequestDetail(req.getId());
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox container = new HBox(btnDetail);
                            container.setStyle("-fx-alignment: CENTER;");
                            setGraphic(container);
                        }
                    }
                };
            }
        });

        // Hỗ trợ click đúp dòng để mở chi tiết
        tblRequests.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ImportRequest selected = tblRequests.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openRequestDetail(selected.getId());
                }
            }
        });

        // 4. Khởi tạo danh mục lọc ComboBox
        cbStatusFilter.setItems(FXCollections.observableArrayList(
            "Tất cả trạng thái", "Chờ xử lý (PENDING)", "Đang xử lý (PROCESSING)", "Đã phê duyệt (APPROVED)", "Bị từ chối (REJECTED)"
        ));
        cbStatusFilter.setValue("Tất cả trạng thái");
        cbStatusFilter.valueProperty().addListener((observable, oldValue, newValue) -> filterRequests(newValue));

        // 5. Tải dữ liệu ban đầu
        loadRequests();
    }

    private void loadRequests() {
        List<ImportRequest> list = requestService.getAllRequests();
        masterData.setAll(list);
        tblRequests.setItems(masterData);
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
    }

    @FXML
    private void handleCreateNewRequest() {
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpbh/create_request.fxml");
    }

    private void openRequestDetail(int requestId) {
        RequestDetailController.selectedRequestId = requestId;
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpbh/request_detail.fxml");
    }
}
