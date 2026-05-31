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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.time.LocalDate;
import java.util.List;

public class CreateRequestController {

    // 1. Controls bên trái: Catalog và Chọn hàng
    @FXML
    private TextField txtSearchProduct;

    @FXML
    private TableView<Merchandise> tblProductCatalog;

    @FXML
    private TableColumn<Merchandise, String> colCatalogCode;

    @FXML
    private TableColumn<Merchandise, String> colCatalogName;

    @FXML
    private TableColumn<Merchandise, String> colCatalogUnit;

    @FXML
    private TextField txtQuantity;

    @FXML
    private DatePicker dpDesiredDate;

    // 2. Controls bên phải: Cart (Giỏ hàng yêu cầu)
    @FXML
    private TableView<ImportRequestItem> tblCartItems;

    @FXML
    private TableColumn<ImportRequestItem, String> colCartCode;

    @FXML
    private TableColumn<ImportRequestItem, String> colCartName;

    @FXML
    private TableColumn<ImportRequestItem, Integer> colCartQty;

    @FXML
    private TableColumn<ImportRequestItem, String> colCartUnit;

    @FXML
    private TableColumn<ImportRequestItem, LocalDate> colCartDate;

    @FXML
    private TableColumn<ImportRequestItem, Void> colCartAction;

    private final MerchandiseService merchandiseService = new MerchandiseService();
    private final ImportRequestService requestService = new ImportRequestService();

    private final ObservableList<Merchandise> catalogData = FXCollections.observableArrayList();
    private final ObservableList<ImportRequestItem> cartData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // --- Setup Table Catalog Trái ---
        colCatalogCode.setCellValueFactory(new PropertyValueFactory<>("merchandiseCode"));
        colCatalogName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCatalogUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

        loadCatalog();

        txtSearchProduct.textProperty().addListener((observable, oldValue, newValue) -> filterCatalog(newValue));

        // --- Setup Table Cart Phải ---
        colCartCode.setCellValueFactory(new PropertyValueFactory<>("merchandiseCode"));
        colCartName.setCellValueFactory(new PropertyValueFactory<>("merchandiseName"));
        colCartQty.setCellValueFactory(new PropertyValueFactory<>("quantityOrdered"));
        colCartUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colCartDate.setCellValueFactory(new PropertyValueFactory<>("desiredDeliveryDate"));

        tblCartItems.setItems(cartData);

        // Nút xóa từng dòng trong giỏ hàng
        colCartAction.setCellFactory(new Callback<TableColumn<ImportRequestItem, Void>, TableCell<ImportRequestItem, Void>>() {
            @Override
            public TableCell<ImportRequestItem, Void> call(final TableColumn<ImportRequestItem, Void> param) {
                return new TableCell<ImportRequestItem, Void>() {
                    private final Button btnDelete = new Button("❌");

                    {
                        btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-padding: 2px; -fx-cursor: hand; -fx-font-weight: bold;");
                        btnDelete.setOnAction(event -> {
                            ImportRequestItem item = getTableView().getItems().get(getIndex());
                            cartData.remove(item);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox container = new HBox(btnDelete);
                            container.setStyle("-fx-alignment: CENTER;");
                            setGraphic(container);
                        }
                    }
                };
            }
        });

        // Thiết lập ngày giao mong muốn mặc định là ngày mai để hỗ trợ người dùng nhanh hơn
        dpDesiredDate.setValue(LocalDate.now().plusDays(1));
    }

    private void loadCatalog() {
        List<Merchandise> list = merchandiseService.getAllActiveMerchandise();
        catalogData.setAll(list);
        tblProductCatalog.setItems(catalogData);
    }

    private void filterCatalog(String query) {
        List<Merchandise> list = merchandiseService.searchActiveMerchandise(query);
        tblProductCatalog.setItems(FXCollections.observableArrayList(list));
    }

    @FXML
    private void handleAddItemToCart() {
        // 1. Kiểm tra sản phẩm được chọn
        Merchandise selected = tblProductCatalog.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Chưa chọn sản phẩm", "Vui lòng chọn một mặt hàng từ Catalog trước khi thêm!");
            return;
        }

        // 2. Kiểm tra số lượng nhập vào
        String qtyText = txtQuantity.getText();
        if (qtyText == null || qtyText.trim().isEmpty()) {
            AlertHelper.showWarning("Số lượng trống", "Vui lòng nhập số lượng cần đặt!");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(qtyText.trim());
            if (quantity <= 0) {
                AlertHelper.showWarning("Số lượng không hợp lệ", "Số lượng mặt hàng đặt mua phải lớn hơn 0!");
                return;
            }
        } catch (NumberFormatException e) {
            AlertHelper.showWarning("Lỗi định dạng", "Số lượng phải là một số nguyên dương hợp lệ!");
            return;
        }

        // 3. Kiểm tra ngày mong muốn
        LocalDate desiredDate = dpDesiredDate.getValue();
        if (desiredDate == null) {
            AlertHelper.showWarning("Chưa chọn ngày", "Vui lòng chọn ngày giao hàng mong muốn!");
            return;
        }

        if (!desiredDate.isAfter(LocalDate.now())) {
            AlertHelper.showWarning("Ngày không hợp lệ", "Ngày giao mong muốn phải ở trong tương lai (sau ngày hôm nay).");
            return;
        }

        // 4. Kiểm tra xem mặt hàng đã có sẵn trong giỏ chưa
        for (ImportRequestItem existingItem : cartData) {
            if (existingItem.getMerchandiseCode().equals(selected.getMerchandiseCode())) {
                // Nếu trùng sản phẩm và trùng ngày -> Cộng dồn số lượng
                if (existingItem.getDesiredDeliveryDate().equals(desiredDate)) {
                    existingItem.setQuantityOrdered(existingItem.getQuantityOrdered() + quantity);
                    tblCartItems.refresh();
                    clearInputs();
                    return;
                } else {
                    // Nếu trùng sản phẩm nhưng khác ngày -> Hỏi ý kiến
                    boolean updateDate = AlertHelper.showConfirm("Trùng sản phẩm khác ngày", 
                        "Mặt hàng này đã có trong giỏ với ngày giao khác. Bạn có muốn cập nhật số lượng và đồng bộ ngày giao thành " + desiredDate + "?");
                    if (updateDate) {
                        existingItem.setQuantityOrdered(existingItem.getQuantityOrdered() + quantity);
                        existingItem.setDesiredDeliveryDate(desiredDate);
                        tblCartItems.refresh();
                        clearInputs();
                    }
                    return;
                }
            }
        }

        // 5. Thêm dòng mới vào giỏ hàng
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
        if (!cartData.isEmpty()) {
            boolean confirm = AlertHelper.showConfirm("Xóa sạch giỏ hàng", "Bạn có chắc chắn muốn xóa tất cả sản phẩm khỏi yêu cầu nhập?");
            if (confirm) {
                cartData.clear();
            }
        }
    }

    @FXML
    private void handleCancel() {
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpbh/request_list.fxml");
    }

    @FXML
    private void handleSubmitRequest() {
        // 1. Kiểm tra giỏ hàng rỗng
        if (cartData.isEmpty()) {
            AlertHelper.showWarning("Giỏ hàng rỗng", "Vui lòng thêm ít nhất một sản phẩm vào yêu cầu trước khi gửi!");
            return;
        }

        // 2. Xác nhận gửi phiếu
        boolean confirm = AlertHelper.showConfirm("Gửi yêu cầu", "Bạn có chắc chắn muốn gửi phiếu yêu cầu nhập hàng này đi xử lý?");
        if (!confirm) {
            return;
        }

        try {
            // 3. Xây dựng đối tượng ImportRequest
            ImportRequest request = new ImportRequest();
            request.setCreatedBy(SessionManager.getInstance().getCurrentUser().getId());
            
            // Chuyển đổi dữ liệu giỏ hàng sang danh sách items của request
            for (ImportRequestItem cartItem : cartData) {
                request.addItem(cartItem);
            }

            // 4. Gọi Service lưu CSDL
            requestService.createImportRequest(request);

            // 5. Hiển thị thông báo thành công và quay lại màn hình danh sách
            AlertHelper.showInfo("Thành công", "Phiếu yêu cầu nhập hàng #" + request.getId() + " đã được lập và gửi đi thành công!");
            NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/bpbh/request_list.fxml");

        } catch (Exception e) {
            AlertHelper.showError("Lỗi hệ thống", "Không thể gửi yêu cầu nhập hàng: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
