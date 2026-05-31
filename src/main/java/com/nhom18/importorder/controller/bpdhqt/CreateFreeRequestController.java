package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.dao.ISiteInventoryDAO;
import com.nhom18.importorder.dao.impl.SQLiteSiteInventoryDAO;
import com.nhom18.importorder.model.entity.*;
import com.nhom18.importorder.model.enums.DeliveryMethod;
import com.nhom18.importorder.model.enums.OrderStatus;
import com.nhom18.importorder.service.MerchandiseService;
import com.nhom18.importorder.service.OrderService;
import com.nhom18.importorder.service.SiteService;
import com.nhom18.importorder.util.AlertHelper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import java.time.LocalDate;
import java.util.*;

public class CreateFreeRequestController {

    // --- Controls bên trái ---
    @FXML
    private DatePicker dpRequiredDate;
    @FXML
    private Label lblTotalQuantity;
    @FXML
    private TableView<ImportRequestItem> tblSelectedItems;
    @FXML
    private TableColumn<ImportRequestItem, String> colItemName;
    @FXML
    private TableColumn<ImportRequestItem, String> colItemCode;
    @FXML
    private TableColumn<ImportRequestItem, Integer> colItemQty;
    @FXML
    private TableColumn<ImportRequestItem, String> colItemUnit;
    @FXML
    private TableColumn<ImportRequestItem, Void> colItemAction;

    @FXML
    private Button btnAutoAllocate;

    // --- Controls bên phải ---
    @FXML
    private Label lblOrdersCount;
    @FXML
    private Button btnSaveAll;
    @FXML
    private Button btnValidate;
    @FXML
    private VBox boxError;
    @FXML
    private Label lblAllocationError;
    @FXML
    private VBox vboxOrdersContainer;

    private final MerchandiseService merchandiseService = new MerchandiseService();
    private final OrderService orderService = new OrderService();
    private final SiteService siteService = new SiteService();
    private final ISiteInventoryDAO siteInventoryDAO = new SQLiteSiteInventoryDAO();
    private boolean isCheckPassed = false;

    private final ObservableList<ImportRequestItem> selectedItemsData = FXCollections.observableArrayList();
    private final ObservableList<Order> proposedOrdersData = FXCollections.observableArrayList();
    
    private final Map<String, Double> merchandisePrices = new HashMap<>();
    private List<Merchandise> activeMerchandise = new ArrayList<>();
    private List<Site> activeSites = new ArrayList<>();

    @FXML
    public void initialize() {
        // --- 1. Thiết lập Cột trái (Mặt hàng yêu cầu) ---
        colItemName.setCellValueFactory(new PropertyValueFactory<>("merchandiseName"));
        colItemCode.setCellValueFactory(new PropertyValueFactory<>("merchandiseCode"));
        colItemQty.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantityOrdered()).asObject());
        colItemUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

        // Nút xóa mặt hàng khỏi bảng yêu cầu
        colItemAction.setCellFactory(new Callback<TableColumn<ImportRequestItem, Void>, TableCell<ImportRequestItem, Void>>() {
            @Override
            public TableCell<ImportRequestItem, Void> call(final TableColumn<ImportRequestItem, Void> param) {
                return new TableCell<ImportRequestItem, Void>() {
                    private final Button btnDelete = new Button("❌");
                    {
                        btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-padding: 2px; -fx-cursor: hand; -fx-font-weight: bold;");
                        btnDelete.setOnAction(event -> {
                            ImportRequestItem item = getTableView().getItems().get(getIndex());
                            selectedItemsData.remove(item);
                            updateTotalQuantityLabel();
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

        tblSelectedItems.setItems(selectedItemsData);

        // --- 2. Nạp dữ liệu Catalog và Sites ---
        activeMerchandise = merchandiseService.getAllActiveMerchandise();
        for (Merchandise m : activeMerchandise) {
            merchandisePrices.put(m.getMerchandiseCode(), m.getPrice());
        }

        activeSites = siteService.getAllActiveSites();

        // Đặt ngày mong muốn mặc định là 10 ngày sau để dễ phân bổ
        dpRequiredDate.setValue(LocalDate.now().plusDays(10));

        updateTotalQuantityLabel();
        isCheckPassed = false;
        btnSaveAll.setDisable(true);
    }

    public void updateTotalQuantityLabel() {
        int total = selectedItemsData.stream().mapToInt(ImportRequestItem::getQuantityOrdered).sum();
        lblTotalQuantity.setText("Tổng: " + total + " đơn vị (" + selectedItemsData.size() + " mặt hàng)");
    }

    @FXML
    private void handleOpenCatalogGrid() {
        openCatalogGrid(null);
    }

    public void openCatalogGrid(Order targetOrder) {
        CatalogGridDialog.show(this, targetOrder);
    }

    @FXML
    private void handleOpenBpbhRequestsPopup() {
        BpbhShortagesDialog.show(this);
    }

    @FXML
    private void handleClearItems() {
        if (!selectedItemsData.isEmpty()) {
            boolean confirm = AlertHelper.showConfirm("Xóa danh sách", "Bạn có chắc chắn muốn xóa tất cả mặt hàng đã chọn?");
            if (confirm) {
                selectedItemsData.clear();
                updateTotalQuantityLabel();
            }
        }
    }

    @FXML
    private void handleAutoAllocate() {
        if (selectedItemsData.isEmpty()) {
            AlertHelper.showWarning("Danh sách trống", "Vui lòng chọn ít nhất một mặt hàng bên cột trái để phân bổ!");
            return;
        }

        LocalDate reqDate = dpRequiredDate.getValue();
        if (reqDate == null) {
            AlertHelper.showWarning("Chưa chọn ngày", "Vui lòng chọn ngày giao hàng mong muốn!");
            return;
        }
        if (!reqDate.isAfter(LocalDate.now())) {
            AlertHelper.showWarning("Ngày giao không hợp lệ", "Ngày giao mong muốn phải ở tương lai!");
            return;
        }

        // Cập nhật ngày nhận mong muốn cho toàn bộ danh sách mặt hàng
        for (ImportRequestItem item : selectedItemsData) {
            item.setDesiredDeliveryDate(reqDate);
        }

        try {
            // Chạy phân bổ in-memory
            List<Order> proposed = orderService.generateProposedOrders(new ArrayList<>(selectedItemsData));
            proposedOrdersData.setAll(proposed);

            // Ẩn panel báo lỗi
            boxError.setVisible(false);
            boxError.setManaged(false);

            // Vẽ các Card đơn hàng sang cột phải
            renderOrderCards();

            // Nếu chỉ phân bổ thì cho phép lưu đơn vì dữ liệu đã hợp lệ
            isCheckPassed = true;
            btnSaveAll.setDisable(false);

        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            // Báo lỗi phân bổ tự động
            lblAllocationError.setText(e.getMessage());
            boxError.setVisible(true);
            boxError.setManaged(true);

            proposedOrdersData.clear();
            vboxOrdersContainer.getChildren().clear();
            lblOrdersCount.setText("Danh Sách Đơn Hàng Đề Xuất (0)");
            
            isCheckPassed = false;
            btnSaveAll.setDisable(true);
        }
    }

    @FXML
    private void handleAddEmptyOrder() {
        if (activeSites.isEmpty()) {
            AlertHelper.showWarning("Lỗi hệ thống", "Không có Site đối tác nào hoạt động!");
            return;
        }

        Order emptyOrder = new Order();
        emptyOrder.setRequestId(-1);
        emptyOrder.setSiteCode(activeSites.get(0).getSiteCode());
        emptyOrder.setSiteName(activeSites.get(0).getName());
        emptyOrder.setDeliveryMethod(DeliveryMethod.SHIP);
        emptyOrder.setStatus(OrderStatus.PENDING);
        emptyOrder.setCreatedDate(LocalDate.now());
        emptyOrder.setEstimatedArrival(LocalDate.now().plusDays(activeSites.get(0).getShipDays()));

        proposedOrdersData.add(emptyOrder);
        renderOrderCards();

        // Thay đổi thủ công yêu cầu kiểm tra trước khi lưu
        isCheckPassed = false;
        btnSaveAll.setDisable(true);
    }

    @FXML
    private void handleSaveAll() {
        if (!isCheckPassed) {
            AlertHelper.showWarning("Chưa kiểm tra", "Vui lòng bấm 'Kiểm Tra' để xác nhận tồn kho trước khi lưu đơn!");
            return;
        }

        if (proposedOrdersData.isEmpty()) {
            AlertHelper.showWarning("Không có đơn hàng", "Vui lòng chạy phân bổ tự động hoặc bấm Add Order để thiết lập đơn hàng trước!");
            return;
        }

        LocalDate reqDate = dpRequiredDate.getValue();
        if (reqDate == null) {
            AlertHelper.showWarning("Chưa chọn ngày", "Vui lòng chọn ngày giao hàng mong muốn!");
            return;
        }

        boolean confirm = AlertHelper.showConfirm("Xác nhận lưu tất cả", 
            "Hệ thống sẽ tiến hành lưu phiếu yêu cầu tự do và tạo đồng thời " + proposedOrdersData.size() + " đơn hàng gửi cho các Site.\nBạn có chắc chắn?");
        
        if (confirm) {
            try {
                // Lưu vào CSDL và trừ tồn kho ảo
                orderService.saveCustomFreeRequestAndOrders(reqDate, new ArrayList<>(proposedOrdersData));
                
                AlertHelper.showInfo("Thành công", "Đã lưu thành công phiếu yêu cầu đặt hàng tự do và tạo " + proposedOrdersData.size() + " đơn đặt hàng gửi Site!");

                // Reset toàn bộ form
                selectedItemsData.clear();
                proposedOrdersData.clear();
                vboxOrdersContainer.getChildren().clear();
                updateTotalQuantityLabel();
                lblOrdersCount.setText("Danh Sách Đơn Hàng Đề Xuất (0)");
                dpRequiredDate.setValue(LocalDate.now().plusDays(10));
                
                isCheckPassed = false;
                btnSaveAll.setDisable(true);

            } catch (Exception e) {
                AlertHelper.showError("Lỗi lưu dữ liệu", "Không thể lưu đơn hàng: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleValidateOrders() {
        if (proposedOrdersData.isEmpty()) {
            AlertHelper.showWarning("Không có đơn hàng", "Vui lòng chạy phân bổ tự động hoặc thêm đơn hàng trước khi kiểm tra!");
            return;
        }

        // Kiểm tra tồn kho của từng sản phẩm tại từng Site đối tác
        for (Order order : proposedOrdersData) {
            String siteCode = order.getSiteCode();
            String siteName = order.getSiteName();

            if (order.getItems().isEmpty()) {
                AlertHelper.showWarning("Đơn hàng rỗng", "Đơn hàng gửi cho Site " + siteName + " chưa có mặt hàng nào!");
                isCheckPassed = false;
                btnSaveAll.setDisable(true);
                return;
            }

            for (OrderItem item : order.getItems()) {
                SiteInventory inventory = siteInventoryDAO.get(siteCode, item.getMerchandiseCode());
                int inStock = (inventory != null) ? inventory.getInStockQuantity() : 0;

                if (inStock < item.getQuantityOrdered()) {
                    AlertHelper.showError("Không đủ tồn kho", 
                        String.format("Site '%s' (%s) không đủ tồn kho cho mặt hàng '%s' (%s)!\n" +
                        "Yêu cầu: %d, Hiện có: %d", 
                        siteName, siteCode, item.getMerchandiseName(), item.getMerchandiseCode(), 
                        item.getQuantityOrdered(), inStock));
                    isCheckPassed = false;
                    btnSaveAll.setDisable(true);
                    return;
                }
            }
        }

        // Nếu tất cả đều hợp lệ
        isCheckPassed = true;
        btnSaveAll.setDisable(false);

        // Cập nhật lại số lượng ở danh sách bên trái (đồng bộ sang trái)
        syncRightToLeft();

        AlertHelper.showInfo("Kiểm tra thành công", 
            "Tất cả các đơn hàng đều hợp lệ và đủ tồn kho tại các Site đối tác!\n" +
            "Danh sách mặt hàng yêu cầu bên trái đã được đồng bộ chính xác.\n" +
            "Bạn có thể tiến hành lưu toàn bộ đơn hàng.");
    }

    private void syncRightToLeft() {
        Map<String, Integer> totalQuantities = new HashMap<>();
        Map<String, String> merchNames = new HashMap<>();
        Map<String, String> merchUnits = new HashMap<>();

        for (Order order : proposedOrdersData) {
            for (OrderItem oi : order.getItems()) {
                String code = oi.getMerchandiseCode();
                totalQuantities.put(code, totalQuantities.getOrDefault(code, 0) + oi.getQuantityOrdered());
                merchNames.put(code, oi.getMerchandiseName());
                merchUnits.put(code, oi.getUnit());
            }
        }

        selectedItemsData.removeIf(item -> !totalQuantities.containsKey(item.getMerchandiseCode()));

        for (Map.Entry<String, Integer> entry : totalQuantities.entrySet()) {
            String code = entry.getKey();
            int qty = entry.getValue();

            boolean exists = false;
            for (ImportRequestItem item : selectedItemsData) {
                if (item.getMerchandiseCode().equals(code)) {
                    item.setQuantityOrdered(qty);
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                ImportRequestItem newItem = new ImportRequestItem();
                newItem.setMerchandiseCode(code);
                newItem.setMerchandiseName(merchNames.get(code));
                newItem.setQuantityOrdered(qty);
                newItem.setUnit(merchUnits.get(code));
                newItem.setDesiredDeliveryDate(dpRequiredDate.getValue() != null ? dpRequiredDate.getValue() : LocalDate.now().plusDays(10));
                selectedItemsData.add(newItem);
            }
        }

        tblSelectedItems.refresh();
        updateTotalQuantityLabel();
    }

    public void renderOrderCards() {
        OrderCardRenderer.render(this);
    }

    public void resetCheckPassed() {
        isCheckPassed = false;
        btnSaveAll.setDisable(true);
    }

    // --- Package-private Getters để phục vụ Lớp Helper ---
    
    List<Merchandise> getActiveMerchandise() {
        return activeMerchandise;
    }

    ObservableList<ImportRequestItem> getSelectedItemsData() {
        return selectedItemsData;
    }

    DatePicker getDpRequiredDate() {
        return dpRequiredDate;
    }

    TableView<ImportRequestItem> getTblSelectedItems() {
        return tblSelectedItems;
    }

    ObservableList<Order> getProposedOrdersData() {
        return proposedOrdersData;
    }

    VBox getVboxOrdersContainer() {
        return vboxOrdersContainer;
    }

    Label getLblOrdersCount() {
        return lblOrdersCount;
    }

    List<Site> getActiveSites() {
        return activeSites;
    }

    Map<String, Double> getMerchandisePrices() {
        return merchandisePrices;
    }
}
