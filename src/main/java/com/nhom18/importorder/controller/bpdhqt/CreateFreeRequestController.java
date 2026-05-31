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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.util.*;

public class CreateFreeRequestController {

    @FXML private DatePicker dpRequiredDate;
    @FXML private Label lblTotalQuantity;
    @FXML private TableView<ImportRequestItem> tblSelectedItems;
    @FXML private TableColumn<ImportRequestItem, String> colItemName;
    @FXML private TableColumn<ImportRequestItem, String> colItemCode;
    @FXML private TableColumn<ImportRequestItem, Integer> colItemQty;
    @FXML private TableColumn<ImportRequestItem, String> colItemUnit;
    @FXML private TableColumn<ImportRequestItem, Void> colItemAction;

    @FXML private Button btnAutoAllocate;
    @FXML private Label lblOrdersCount;
    @FXML private Button btnSaveAll;
    @FXML private Button btnValidate;
    @FXML private VBox boxError;
    @FXML private Label lblAllocationError;
    @FXML private VBox vboxOrdersContainer;

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
        CreateFreeRequestTableHelper.setupColumns(
            tblSelectedItems, colItemName, colItemCode, colItemQty, colItemUnit, colItemAction,
            selectedItemsData, this
        );

        activeMerchandise = merchandiseService.getAllActiveMerchandise();
        for (Merchandise m : activeMerchandise) {
            merchandisePrices.put(m.getMerchandiseCode(), m.getPrice());
        }
        activeSites = siteService.getAllActiveSites();
        dpRequiredDate.setValue(LocalDate.now().plusDays(10));
        updateTotalQuantityLabel();
        resetCheckPassed();
    }

    public void updateTotalQuantityLabel() {
        int total = selectedItemsData.stream().mapToInt(ImportRequestItem::getQuantityOrdered).sum();
        lblTotalQuantity.setText("Tổng: " + total + " đơn vị (" + selectedItemsData.size() + " mặt hàng)");
    }

    @FXML private void handleOpenCatalogGrid() { openCatalogGrid(null); }
    public void openCatalogGrid(Order targetOrder) { CatalogGridDialog.show(this, targetOrder); }
    @FXML private void handleOpenBpbhRequestsPopup() { BpbhShortagesDialog.show(this); }

    @FXML
    private void handleClearItems() {
        if (!selectedItemsData.isEmpty() && AlertHelper.showConfirm("Xóa danh sách", "Bạn có chắc chắn muốn xóa tất cả mặt hàng đã chọn?")) {
            selectedItemsData.clear();
            updateTotalQuantityLabel();
        }
    }

    @FXML private void handleAutoAllocate() { FreeRequestAllocator.allocate(this); }

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
        resetCheckPassed();
    }

    @FXML private void handleSaveAll() { FreeRequestSaver.save(this); }
    @FXML private void handleValidateOrders() { FreeRequestValidator.validate(this); }

    public void renderOrderCards() { OrderCardRenderer.render(this); }
    
    public void resetCheckPassed() {
        isCheckPassed = false;
        btnSaveAll.setDisable(true);
    }

    public boolean isCheckPassed() { return isCheckPassed; }
    public void setCheckPassed(boolean val) { this.isCheckPassed = val; }
    OrderService getOrderService() { return orderService; }
    ISiteInventoryDAO getSiteInventoryDAO() { return siteInventoryDAO; }
    DatePicker getDpRequiredDate() { return dpRequiredDate; }
    TableView<ImportRequestItem> getTblSelectedItems() { return tblSelectedItems; }
    ObservableList<ImportRequestItem> getSelectedItemsData() { return selectedItemsData; }
    ObservableList<Order> getProposedOrdersData() { return proposedOrdersData; }
    VBox getVboxOrdersContainer() { return vboxOrdersContainer; }
    Label getLblOrdersCount() { return lblOrdersCount; }
    Button getBtnSaveAll() { return btnSaveAll; }
    VBox getBoxError() { return boxError; }
    Label getLblAllocationError() { return lblAllocationError; }
    List<Merchandise> getActiveMerchandise() { return activeMerchandise; }
    List<Site> getActiveSites() { return activeSites; }
    Map<String, Double> getMerchandisePrices() { return merchandisePrices; }
}
