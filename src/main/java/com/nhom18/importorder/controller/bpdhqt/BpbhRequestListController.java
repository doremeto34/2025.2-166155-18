package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.dao.IImportRequestDAO;
import com.nhom18.importorder.dao.impl.SQLiteImportRequestDAO;
import com.nhom18.importorder.model.entity.ImportRequest;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.enums.RequestStatus;
import com.nhom18.importorder.service.ImportRequestService;
import com.nhom18.importorder.util.AlertHelper;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class BpbhRequestListController {

    @FXML private ComboBox<String> cbStatusFilter;
    @FXML private TableView<ImportRequest> tblRequests;
    @FXML private TableColumn<ImportRequest, Integer> colId;
    @FXML private TableColumn<ImportRequest, String> colCreator, colDate;
    @FXML private TableColumn<ImportRequest, RequestStatus> colStatus;

    @FXML private TableView<ImportRequestItem> tblRequestItems;
    @FXML private TableColumn<ImportRequestItem, String> colItemMerch, colItemName, colItemUnit, colItemDate;
    @FXML private TableColumn<ImportRequestItem, Integer> colItemQty;

    @FXML private HBox hboxActions;
    @FXML private Button btnApprove, btnReject;
    @FXML private GridPane gridRequestDetail;
    @FXML private Label lblReqId, lblReqCreatorDate;

    private final IImportRequestDAO requestDAO = new SQLiteImportRequestDAO();
    private final ImportRequestService requestService = new ImportRequestService();
    private final ObservableList<ImportRequest> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        BpbhRequestTableHelper.setupRequestColumns(colId, colCreator, colDate, colStatus);
        BpbhRequestTableHelper.setupItemColumns(colItemMerch, colItemName, colItemQty, colItemUnit, colItemDate);

        tblRequests.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadRequestItems(newVal.getId());
                gridRequestDetail.setVisible(true); gridRequestDetail.setManaged(true);
                lblReqId.setText("#" + newVal.getId());
                lblReqCreatorDate.setText(newVal.getCreatorName() + " | Ngày: " + newVal.getCreatedDate().toString());
                hboxActions.setVisible(true); hboxActions.setManaged(true);
                
                boolean isPending = (newVal.getStatus() == RequestStatus.PENDING);
                btnApprove.setDisable(!isPending); btnReject.setDisable(!isPending);
            } else {
                tblRequestItems.getItems().clear();
                gridRequestDetail.setVisible(false); gridRequestDetail.setManaged(false);
                hboxActions.setVisible(false); hboxActions.setManaged(false);
            }
        });

        cbStatusFilter.setItems(FXCollections.observableArrayList("Tất cả trạng thái", "Chờ xử lý (PENDING)", "Đang xử lý (PROCESSING)", "Đã phê duyệt (APPROVED)", "Bị từ chối (REJECTED)"));
        cbStatusFilter.setValue("Tất cả trạng thái");
        cbStatusFilter.valueProperty().addListener((observable, oldValue, newValue) -> filterRequests(newValue));

        loadRequests();
    }

    private void loadRequests() {
        masterData.setAll(requestDAO.getAllWithCreatorName());
        tblRequests.setItems(masterData);
    }

    private void loadRequestItems(int requestId) {
        ImportRequest fullRequest = requestDAO.getById(requestId);
        if (fullRequest != null) tblRequestItems.setItems(FXCollections.observableArrayList(fullRequest.getItems()));
        else tblRequestItems.getItems().clear();
    }

    private void filterRequests(String filterOption) {
        if (filterOption == null || filterOption.equals("Tất cả trạng thái")) {
            tblRequests.setItems(masterData); return;
        }
        RequestStatus targetStatus = switch (filterOption) {
            case "Chờ xử lý (PENDING)" -> RequestStatus.PENDING;
            case "Đang xử lý (PROCESSING)" -> RequestStatus.PROCESSING;
            case "Đã phê duyệt (APPROVED)" -> RequestStatus.APPROVED;
            case "Bị từ chối (REJECTED)" -> RequestStatus.REJECTED;
            default -> null;
        };
        if (targetStatus == null) tblRequests.setItems(masterData);
        else {
            List<ImportRequest> filtered = masterData.stream().filter(r -> r.getStatus() == targetStatus).collect(Collectors.toList());
            tblRequests.setItems(FXCollections.observableArrayList(filtered));
        }
    }

    @FXML
    private void handleRefresh() {
        loadRequests();
        cbStatusFilter.setValue("Tất cả trạng thái");
        tblRequestItems.getItems().clear();
        gridRequestDetail.setVisible(false); gridRequestDetail.setManaged(false);
        hboxActions.setVisible(false); hboxActions.setManaged(false);
    }

    @FXML
    private void handleApprove() {
        ImportRequest selected = tblRequests.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (AlertHelper.showConfirm("Xác nhận duyệt", "Bạn có chắc chắn muốn duyệt phiếu yêu cầu nhập hàng này?")) {
            try {
                requestDAO.updateStatus(selected.getId(), RequestStatus.APPROVED);
                AlertHelper.showInfo("Thành công", "Đã duyệt phiếu yêu cầu nhập hàng thành công!");
                loadRequests();
                masterData.stream().filter(r -> r.getId() == selected.getId()).findFirst().ifPresent(r -> tblRequests.getSelectionModel().select(r));
            } catch (Exception e) {
                AlertHelper.showError("Lỗi", "Không thể duyệt yêu cầu: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleReject() {
        ImportRequest selected = tblRequests.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (AlertHelper.showConfirm("Xác nhận từ chối", "Bạn có chắc chắn muốn từ chối phiếu yêu cầu nhập hàng này?")) {
            try {
                requestService.rejectImportRequest(selected.getId());
                AlertHelper.showInfo("Thành công", "Đã từ chối phiếu yêu cầu nhập hàng và hoàn trả tồn kho nội bộ!");
                loadRequests();
                masterData.stream().filter(r -> r.getId() == selected.getId()).findFirst().ifPresent(r -> tblRequests.getSelectionModel().select(r));
            } catch (Exception e) {
                AlertHelper.showError("Lỗi", "Không thể từ chối yêu cầu: " + e.getMessage());
            }
        }
    }
}
