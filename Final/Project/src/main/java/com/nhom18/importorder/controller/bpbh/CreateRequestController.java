package com.nhom18.importorder.controller.bpbh;

import com.nhom18.importorder.model.entity.ImportRequest;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.entity.Merchandise;
import com.nhom18.importorder.service.ImportRequestService;
import com.nhom18.importorder.service.MerchandiseService;
import com.nhom18.importorder.util.AlertHelper;
import com.nhom18.importorder.util.NavigationManager;
import com.nhom18.importorder.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDate;
import java.util.List;

public class CreateRequestController {

    @FXML private TextField txtSearchProduct, txtQuantity;
    @FXML private DatePicker dpDesiredDate;
    @FXML private TableView<Merchandise> tblProductCatalog;
    @FXML private TableColumn<Merchandise, String> colCatalogCode, colCatalogName, colCatalogUnit;

    @FXML private TableView<ImportRequestItem> tblCartItems;
    @FXML private TableColumn<ImportRequestItem, String> colCartCode, colCartName, colCartUnit;
    @FXML private TableColumn<ImportRequestItem, Integer> colCartQty;
    @FXML private TableColumn<ImportRequestItem, LocalDate> colCartDate;
    @FXML private TableColumn<ImportRequestItem, Void> colCartAction;

    private final MerchandiseService merchandiseService = new MerchandiseService();
    private final ImportRequestService requestService = new ImportRequestService();

    private final ObservableList<Merchandise> catalogData = FXCollections.observableArrayList();
    private final ObservableList<ImportRequestItem> cartData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        RequestTableHelper.setupCatalogColumns(colCatalogCode, colCatalogName, colCatalogUnit);
        RequestTableHelper.setupCartColumns(colCartCode, colCartName, colCartQty, colCartUnit, colCartDate, colCartAction, cartData);
        tblCartItems.setItems(cartData);

        loadCatalog();
        txtSearchProduct.textProperty().addListener((observable, oldValue, newValue) -> filterCatalog(newValue));
        dpDesiredDate.setValue(LocalDate.now().plusDays(1));
    }

    private void loadCatalog() {
        catalogData.setAll(merchandiseService.getAllActiveMerchandise());
        tblProductCatalog.setItems(catalogData);
    }

    private void filterCatalog(String query) {
        tblProductCatalog.setItems(FXCollections.observableArrayList(merchandiseService.searchActiveMerchandise(query)));
    }

    @FXML
    private void handleAddItemToCart() {
        Merchandise selected = tblProductCatalog.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Chưa chọn sản phẩm", "Vui lòng chọn một mặt hàng từ Catalog!"); return;
        }

        String qtyText = txtQuantity.getText();
        if (qtyText == null || qtyText.trim().isEmpty()) {
            AlertHelper.showWarning("Số lượng trống", "Vui lòng nhập số lượng!"); return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(qtyText.trim());
            if (quantity <= 0) { AlertHelper.showWarning("Số lượng không hợp lệ", "Số lượng phải lớn hơn 0!"); return; }
        } catch (NumberFormatException e) {
            AlertHelper.showWarning("Lỗi định dạng", "Số lượng phải là một số nguyên dương!"); return;
        }

        LocalDate desiredDate = dpDesiredDate.getValue();
        if (desiredDate == null) { AlertHelper.showWarning("Chưa chọn ngày", "Vui lòng chọn ngày giao mong muốn!"); return; }
        if (!desiredDate.isAfter(LocalDate.now())) {
            AlertHelper.showWarning("Ngày không hợp lệ", "Ngày giao mong muốn phải ở trong tương lai."); return;
        }

        for (ImportRequestItem existingItem : cartData) {
            if (existingItem.getMerchandiseCode().equals(selected.getMerchandiseCode())) {
                if (existingItem.getDesiredDeliveryDate().equals(desiredDate)) {
                    existingItem.setQuantityOrdered(existingItem.getQuantityOrdered() + quantity);
                    tblCartItems.refresh(); clearInputs(); return;
                } else {
                    if (AlertHelper.showConfirm("Trùng sản phẩm", "Đã có trong giỏ với ngày khác. Cập nhật và đồng bộ ngày giao thành " + desiredDate + "?")) {
                        existingItem.setQuantityOrdered(existingItem.getQuantityOrdered() + quantity);
                        existingItem.setDesiredDeliveryDate(desiredDate);
                        tblCartItems.refresh(); clearInputs();
                    }
                    return;
                }
            }
        }

        ImportRequestItem newItem = new ImportRequestItem();
        newItem.setMerchandiseCode(selected.getMerchandiseCode());
        newItem.setMerchandiseName(selected.getName());
        newItem.setQuantityOrdered(quantity);
        newItem.setUnit(selected.getUnit());
        newItem.setDesiredDeliveryDate(desiredDate);

        cartData.add(newItem);
        clearInputs();
    }

    private void clearInputs() {
        tblProductCatalog.getSelectionModel().clearSelection();
        txtQuantity.clear();
        dpDesiredDate.setValue(LocalDate.now().plusDays(1));
    }

    @FXML
    private void handleClearCart() {
        if (!cartData.isEmpty() && AlertHelper.showConfirm("Xóa sạch giỏ hàng", "Bạn có chắc chắn muốn xóa tất cả sản phẩm khỏi yêu cầu nhập?")) {
            cartData.clear();
        }
    }

    @FXML private void handleCancel() {
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpbh/request_list.fxml");
    }

    @FXML
    private void handleSubmitRequest() {
        if (cartData.isEmpty()) { AlertHelper.showWarning("Giỏ hàng rỗng", "Vui lòng thêm ít nhất một sản phẩm!"); return; }
        if (!AlertHelper.showConfirm("Gửi yêu cầu", "Bạn có chắc chắn muốn gửi phiếu yêu cầu nhập hàng này?")) return;

        try {
            ImportRequest request = new ImportRequest();
            request.setCreatedBy(SessionManager.getInstance().getCurrentUser().getId());
            for (ImportRequestItem cartItem : cartData) request.addItem(cartItem);
            
            requestService.createImportRequest(request);
            AlertHelper.showInfo("Thành công", "Phiếu yêu cầu nhập hàng #" + request.getId() + " đã được gửi thành công!");
            NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpbh/request_list.fxml");
        } catch (Exception e) {
            AlertHelper.showError("Lỗi hệ thống", "Không thể gửi yêu cầu nhập hàng: " + e.getMessage());
        }
    }
}
