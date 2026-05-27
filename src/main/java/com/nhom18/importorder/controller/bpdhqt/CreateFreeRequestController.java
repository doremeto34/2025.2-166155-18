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
import com.nhom18.importorder.util.NavigationManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
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

    private void updateTotalQuantityLabel() {
        int total = selectedItemsData.stream().mapToInt(ImportRequestItem::getQuantityOrdered).sum();
        lblTotalQuantity.setText("Tổng: " + total + " đơn vị (" + selectedItemsData.size() + " mặt hàng)");
    }

    /**
     * Mở màn hình Catalog dưới dạng Grid View cao cấp, cho phép lọc tìm kiếm và thêm mặt hàng kèm hiệu ứng.
     */
    @FXML
    private void handleOpenCatalogGrid() {
        openCatalogGrid(null);
    }

    private void openCatalogGrid(Order targetOrder) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(targetOrder == null ? "Catalog Mặt Hàng" : "Thêm Mặt Hàng vào Đơn Hàng");
        dialogStage.setMinWidth(800);
        dialogStage.setMinHeight(700);
        dialogStage.setHeight(700);

        VBox root = new VBox(15);
        root.setStyle("-fx-padding: 20px; -fx-background-color: #f8fafc;");

        // Header & Search
        Label title = new Label(targetOrder == null ? "Danh Mục Mặt Hàng Catalog" : "Thêm Mặt Hàng vào Đơn Hàng");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: -text-primary;");

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("🔍 Tìm kiếm theo tên hoặc mã mặt hàng...");
        txtSearch.setPrefWidth(350);
        txtSearch.setStyle("-fx-background-radius: 6px; -fx-padding: 8px 12px; -fx-border-color: #cbd5e1;");

        HBox header = new HBox(15, title, txtSearch);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);
        root.getChildren().add(header);

        // ScrollPane & TilePane for GridView
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        TilePane tilePane = new TilePane();
        tilePane.setHgap(15);
        tilePane.setVgap(15);
        tilePane.setPadding(new Insets(5));
        tilePane.setPrefColumns(3); // Hiển thị 3 cột
        scroll.setContent(tilePane);
        root.getChildren().add(scroll);

        // Hàm render động Grid các Card mặt hàng
        Runnable renderGrid = () -> {
            tilePane.getChildren().clear();
            String query = txtSearch.getText().toLowerCase().trim();

            for (Merchandise m : activeMerchandise) {
                if (!query.isEmpty() && !m.getName().toLowerCase().contains(query) && !m.getMerchandiseCode().toLowerCase().contains(query)) {
                    continue; // Ẩn nếu không khớp
                }

                VBox card = new VBox(8);
                card.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-padding: 15px; -fx-alignment: CENTER;");
                card.setPrefWidth(225);

                Label name = new Label(m.getName());
                name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -text-primary; -fx-alignment: CENTER;");
                name.setWrapText(true);
                name.setMinHeight(40);

                Label code = new Label("Mã: " + m.getMerchandiseCode());
                code.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 12px;");

                Label price = new Label(String.format("%,.2f USD / %s", m.getPrice(), m.getUnit()));
                price.setStyle("-fx-text-fill: -accent-indigo; -fx-font-weight: bold; -fx-font-size: 13px;");

                // Ô nhập số lượng và nút Thêm
                TextField qtyInput = new TextField("1");
                qtyInput.setPrefWidth(45.0);
                qtyInput.setStyle("-fx-alignment: CENTER;");

                Button btnAdd = new Button("Thêm");
                btnAdd.getStyleClass().addAll("button-primary");
                btnAdd.setStyle("-fx-font-size: 12px; -fx-padding: 4px 12px;");
                btnAdd.setOnAction(evt -> {
                    try {
                        int q = Integer.parseInt(qtyInput.getText().trim());
                        if (q > 0) {
                            if (targetOrder == null) {
                                // THÊM VÀO BÊN TRÁI
                                boolean duplicate = false;
                                for (ImportRequestItem item : selectedItemsData) {
                                    if (item.getMerchandiseCode().equals(m.getMerchandiseCode())) {
                                        item.setQuantityOrdered(item.getQuantityOrdered() + q);
                                        duplicate = true;
                                        break;
                                    }
                                }

                                if (!duplicate) {
                                    ImportRequestItem newItem = new ImportRequestItem();
                                    newItem.setMerchandiseCode(m.getMerchandiseCode());
                                    newItem.setMerchandiseName(m.getName());
                                    newItem.setQuantityOrdered(q);
                                    newItem.setUnit(m.getUnit());
                                    newItem.setDesiredDeliveryDate(dpRequiredDate.getValue() != null ? dpRequiredDate.getValue() : LocalDate.now().plusDays(10));
                                    selectedItemsData.add(newItem);
                                }

                                tblSelectedItems.refresh();
                                updateTotalQuantityLabel();
                            } else {
                                // THÊM VÀO BÊN PHẢI (Specific Order)
                                boolean duplicate = false;
                                for (OrderItem existingItem : targetOrder.getItems()) {
                                    if (existingItem.getMerchandiseCode().equals(m.getMerchandiseCode())) {
                                        existingItem.setQuantityOrdered(existingItem.getQuantityOrdered() + q);
                                        duplicate = true;
                                        break;
                                    }
                                }

                                if (!duplicate) {
                                    OrderItem newOi = new OrderItem();
                                    newOi.setMerchandiseCode(m.getMerchandiseCode());
                                    newOi.setMerchandiseName(m.getName());
                                    newOi.setQuantityOrdered(q);
                                    newOi.setQuantityConfirmed(0);
                                    newOi.setQuantityReceived(0);
                                    newOi.setUnit(m.getUnit());
                                    targetOrder.addItem(newOi);
                                }

                                // Cập nhật UI
                                renderOrderCards();
                                isCheckPassed = false;
                                btnSaveAll.setDisable(true);
                            }

                            // Đổi màu nút để báo hiệu thành công
                            btnAdd.setText("✓ Đã Thêm");
                            btnAdd.setStyle("-fx-background-color: #10b981; -fx-font-size: 12px; -fx-padding: 4px 12px;");
                            
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    Platform.runLater(() -> {
                                        btnAdd.setText("Thêm");
                                        btnAdd.setStyle("");
                                        btnAdd.getStyleClass().addAll("button-primary");
                                    });
                                }
                            }, 1000);

                        }
                    } catch (NumberFormatException ignored) {
                        AlertHelper.showWarning("Lỗi", "Số lượng phải là số nguyên dương.");
                    }
                });

                HBox actionBox = new HBox(6, new Label("SL:"), qtyInput, btnAdd);
                actionBox.setAlignment(Pos.CENTER);
                
                card.getChildren().addAll(name, code, price, actionBox);
                tilePane.getChildren().add(card);
            }
        };

        // Lắng nghe tìm kiếm
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> renderGrid.run());
        renderGrid.run();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
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

            // Nếu đơn hàng rỗng hoặc không có mặt hàng nào
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

    /**
     * Đồng bộ hóa số lượng mặt hàng từ cột bên phải (các đơn hàng) ngược lại cột bên trái (yêu cầu đặt hàng gốc).
     * Điều này bảo đảm Two-way Data Synchronization hoàn hảo.
     */
    private void syncRightToLeft() {
        Map<String, Integer> totalQuantities = new HashMap<>();
        Map<String, String> merchNames = new HashMap<>();
        Map<String, String> merchUnits = new HashMap<>();

        // 1. Tích lũy số lượng trên tất cả các đơn hàng bên phải
        for (Order order : proposedOrdersData) {
            for (OrderItem oi : order.getItems()) {
                String code = oi.getMerchandiseCode();
                totalQuantities.put(code, totalQuantities.getOrDefault(code, 0) + oi.getQuantityOrdered());
                merchNames.put(code, oi.getMerchandiseName());
                merchUnits.put(code, oi.getUnit());
            }
        }

        // 2. Đồng bộ hóa sang selectedItemsData bên trái
        // Xóa những mặt hàng không còn tồn tại bên phải (đã bị xóa sạch)
        selectedItemsData.removeIf(item -> !totalQuantities.containsKey(item.getMerchandiseCode()));

        // Cập nhật số lượng hoặc thêm mới mặt hàng nếu bên phải tự bổ sung thủ công
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

    /**
     * Hàm vẽ động toàn bộ danh sách đơn hàng sang cột phải dưới dạng các Card sang trọng và binding dữ liệu trực tiếp.
     */
    private void renderOrderCards() {
        vboxOrdersContainer.getChildren().clear();
        lblOrdersCount.setText("Danh Sách Đơn Hàng Đề Xuất (" + proposedOrdersData.size() + ")");

        for (int i = 0; i < proposedOrdersData.size(); i++) {
            final int index = i;
            Order order = proposedOrdersData.get(index);
            
            // --- 1. Tạo Card Container ---
            VBox card = new VBox();
            card.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-padding: 15px; -fx-spacing: 12px;");
            
            // --- 2. Card Header ---
            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            Label lblOrderNo = new Label("Đơn Đặt Hàng #" + (index + 1));
            lblOrderNo.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: -text-primary;");
            HBox.setHgrow(lblOrderNo, Priority.ALWAYS);

            Button btnDeleteOrder = new Button("🗑️ Delete");
            btnDeleteOrder.getStyleClass().addAll("button-secondary");
            btnDeleteOrder.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-padding: 4px 10px;");
            btnDeleteOrder.setOnAction(event -> {
                proposedOrdersData.remove(index);
                renderOrderCards();
                isCheckPassed = false;
                btnSaveAll.setDisable(true);
            });
            header.getChildren().addAll(lblOrderNo, btnDeleteOrder);
            card.getChildren().add(header);

            // --- 3. Form Chọn Site & Phương Thức Vận Chuyển ---
            GridPane formGrid = new GridPane();
            formGrid.setHgap(15);
            formGrid.setVgap(10);

            // Label Site
            Label lblSite = new Label("Site đối tác:");
            lblSite.setStyle("-fx-font-weight: bold; -fx-text-fill: -text-secondary; -fx-font-size: 12px;");
            formGrid.add(lblSite, 0, 0);

            // ComboBox Site
            ComboBox<Site> cbSite = new ComboBox<>(FXCollections.observableArrayList(activeSites));
            cbSite.setPrefWidth(280.0);
            // Tìm Site hiện tại của đơn hàng để chọn mặc định
            for (Site s : activeSites) {
                if (s.getSiteCode().equals(order.getSiteCode())) {
                    cbSite.setValue(s);
                    break;
                }
            }
            cbSite.setConverter(new StringConverter<Site>() {
                @Override
                public String toString(Site s) {
                    return s == null ? "" : s.getName() + " (" + s.getSiteCode() + ")";
                }
                @Override
                public Site fromString(String string) {
                    return null;
                }
            });
            formGrid.add(cbSite, 1, 0);

            // Label Method
            Label lblMethod = new Label("Vận chuyển:");
            lblMethod.setStyle("-fx-font-weight: bold; -fx-text-fill: -text-secondary; -fx-font-size: 12px;");
            formGrid.add(lblMethod, 0, 1);

            // ComboBox Method
            ComboBox<String> cbMethod = new ComboBox<>();
            cbMethod.setPrefWidth(280.0);
            
            formGrid.add(cbMethod, 1, 1);
            card.getChildren().add(formGrid);

            // Dòng tóm tắt đơn hàng (Tổng tiền, số mặt hàng, ngày đến dự kiến)
            Label lblSummary = new Label();
            lblSummary.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 13px;");
            card.getChildren().add(lblSummary);

            // --- 4. Danh sách các mặt hàng trong đơn hàng ---
            VBox vboxItems = new VBox();
            vboxItems.setSpacing(8.0);
            vboxItems.setStyle("-fx-background-color: rgba(248,250,252,0.6); -fx-background-radius: 6px; -fx-padding: 10px;");
            
            // Hàm cập nhật ComboBox Vận Chuyển và thông tin Ngày đến/Tóm tắt đơn hàng
            Runnable updateOrderMetrics = () -> {
                Site selectedSite = cbSite.getValue();
                if (selectedSite != null) {
                    order.setSiteCode(selectedSite.getSiteCode());
                    order.setSiteName(selectedSite.getName());

                    // Cập nhật danh sách phương án vận chuyển kèm số ngày
                    cbMethod.setItems(FXCollections.observableArrayList(
                        "Đường biển (SHIP - " + selectedSite.getShipDays() + " ngày)",
                        "Đường hàng không (AIR - " + selectedSite.getAirDays() + " ngày)"
                    ));

                    // Đồng bộ phương thức vận chuyển của đơn hàng
                    if (order.getDeliveryMethod() == DeliveryMethod.SHIP) {
                        cbMethod.setValue("Đường biển (SHIP - " + selectedSite.getShipDays() + " ngày)");
                        order.setEstimatedArrival(LocalDate.now().plusDays(selectedSite.getShipDays()));
                    } else {
                        cbMethod.setValue("Đường hàng không (AIR - " + selectedSite.getAirDays() + " ngày)");
                        order.setEstimatedArrival(LocalDate.now().plusDays(selectedSite.getAirDays()));
                    }
                }

                // Tính toán tổng tiền và số mặt hàng
                double totalVal = 0;
                int itemsCount = 0;
                for (OrderItem oi : order.getItems()) {
                    double pr = merchandisePrices.getOrDefault(oi.getMerchandiseCode(), 0.0);
                    totalVal += pr * oi.getQuantityOrdered();
                    itemsCount += oi.getQuantityOrdered();
                }

                int days = (order.getDeliveryMethod() == DeliveryMethod.SHIP && selectedSite != null) ? selectedSite.getShipDays() : (selectedSite != null ? selectedSite.getAirDays() : 0);
                lblSummary.setText(String.format("Tổng tiền: %,.2f USD  •  %d sản phẩm  •  Giao hàng: %d ngày (Dự kiến đến: %s)", 
                    totalVal, itemsCount, days, order.getEstimatedArrival().toString()));
            };

            // Lắng nghe đổi Site để cập nhật lại thông tin vận chuyển
            cbSite.valueProperty().addListener((obs, oldSite, newSite) -> {
                if (newSite != null) {
                    order.setSiteCode(newSite.getSiteCode());
                    order.setSiteName(newSite.getName());
                    updateOrderMetrics.run();
                    if (oldSite != null) {
                        isCheckPassed = false;
                        btnSaveAll.setDisable(true);
                    }
                }
            });

            // Lắng nghe đổi phương thức vận chuyển
            cbMethod.valueProperty().addListener((obs, oldMethod, newMethod) -> {
                if (newMethod != null) {
                    if (newMethod.contains("SHIP")) {
                        order.setDeliveryMethod(DeliveryMethod.SHIP);
                    } else {
                        order.setDeliveryMethod(DeliveryMethod.AIR);
                    }
                    updateOrderMetrics.run();
                    if (oldMethod != null) {
                        isCheckPassed = false;
                        btnSaveAll.setDisable(true);
                    }
                }
            });

            // Gọi chạy ban đầu để bind dữ liệu đầy đủ
            updateOrderMetrics.run();

            // Hàm vẽ danh sách mặt hàng của Card
            Runnable renderItemsList = new Runnable() {
                @Override
                public void run() {
                    vboxItems.getChildren().clear();
                    if (order.getItems().isEmpty()) {
                        vboxItems.getChildren().add(new Label("Chưa có mặt hàng trong đơn hàng này."));
                    } else {
                        for (int k = 0; k < order.getItems().size(); k++) {
                            final int itemIndex = k;
                            OrderItem item = order.getItems().get(itemIndex);

                            HBox itemRow = new HBox();
                            itemRow.setAlignment(Pos.CENTER_LEFT);
                            itemRow.setSpacing(10.0);

                            // Thông tin tên hàng và đơn giá
                            double unitPrice = merchandisePrices.getOrDefault(item.getMerchandiseCode(), 0.0);
                            VBox itemInfo = new VBox(2.0);
                            Label lblName = new Label(item.getMerchandiseName());
                            lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                            
                            Label lblPriceDetail = new Label(String.format("Mã: %s  •  Đơn giá: %,.2f USD / %s", 
                                item.getMerchandiseCode(), unitPrice, item.getUnit()));
                            lblPriceDetail.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 11px;");
                            itemInfo.getChildren().addAll(lblName, lblPriceDetail);
                            HBox.setHgrow(itemInfo, Priority.ALWAYS);

                            // TextField nhập số lượng
                            TextField txtQtyInput = new TextField(String.valueOf(item.getQuantityOrdered()));
                            txtQtyInput.setPrefWidth(65.0);
                            txtQtyInput.setStyle("-fx-alignment: CENTER;");
                            txtQtyInput.textProperty().addListener((obs, oldVal, newVal) -> {
                                if (newVal != null && !newVal.trim().isEmpty()) {
                                    try {
                                        int newQty = Integer.parseInt(newVal.trim());
                                        if (newQty > 0) {
                                            item.setQuantityOrdered(newQty);
                                            updateOrderMetrics.run();
                                            isCheckPassed = false;
                                            btnSaveAll.setDisable(true);
                                        }
                                    } catch (NumberFormatException ignored) {}
                                }
                            });

                            // Nút xóa dòng hàng
                            Button btnDeleteItem = new Button("❌");
                            btnDeleteItem.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-font-size: 12px;");
                            btnDeleteItem.setOnAction(event -> {
                                order.getItems().remove(itemIndex);
                                this.run();
                                updateOrderMetrics.run();
                                isCheckPassed = false;
                                btnSaveAll.setDisable(true);
                            });

                            itemRow.getChildren().addAll(itemInfo, txtQtyInput, btnDeleteItem);
                            vboxItems.getChildren().add(itemRow);
                        }
                    }
                }
            };

            // Khởi chạy render mặt hàng ban đầu
            renderItemsList.run();
            card.getChildren().add(vboxItems);

            // --- 5. Nút Thêm Mặt Hàng Từ Catalog của Card đơn hàng ---
            Button btnAddItemToOrder = new Button("➕ Add Item from Catalog");
            btnAddItemToOrder.getStyleClass().addAll("button-secondary");
            btnAddItemToOrder.setStyle("-fx-font-size: 12px; -fx-padding: 4px 12px; -fx-background-color: transparent; -fx-border-color: #cbd5e1; -fx-border-radius: 4px;");
            btnAddItemToOrder.setOnAction(event -> {
                openCatalogGrid(order);
            });

            card.getChildren().add(btnAddItemToOrder);

            // Add Card vào VBox Container
            vboxOrdersContainer.getChildren().add(card);
        }
    }
}
