package com.nhom18.importorder.controller.site;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.enums.OrderStatus;
import com.nhom18.importorder.service.OrderService;
import com.nhom18.importorder.util.AlertHelper;
import com.nhom18.importorder.util.NavigationManager;
import com.nhom18.importorder.util.SessionManager;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class SiteOrderListController {

    @FXML
    private TableView<Order> tblOrders;
    @FXML
    private TableColumn<Order, Integer> colOrderId;
    @FXML
    private TableColumn<Order, Integer> colReqId;
    @FXML
    private TableColumn<Order, String> colDeliveryMethod;
    @FXML
    private TableColumn<Order, String> colCreatedDate;
    @FXML
    private TableColumn<Order, String> colEstArrival;
    @FXML
    private TableColumn<Order, String> colStatus;

    @FXML
    private ComboBox<String> cbOrderStatusFilter;
    @FXML
    private Button btnViewDetail;

    private final OrderService orderService;
    private List<Order> siteOrdersList;
    private String currentSiteCode;

    public SiteOrderListController() {
        this.orderService = new OrderService();
    }

    @FXML
    public void initialize() {
        // Lấy thông tin Site của User hiện tại
        if (SessionManager.getInstance().getCurrentUser() != null) {
            currentSiteCode = SessionManager.getInstance().getCurrentUser().getSiteCode();
        }

        // 1. Ánh xạ dữ liệu cột TableView
        colOrderId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        colReqId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getRequestId()).asObject());
        colDeliveryMethod.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDeliveryMethod().name()));
        colCreatedDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedDate().toString()));
        colEstArrival.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstimatedArrival().toString()));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));

        // Tự động tô màu trạng thái để đảm bảo thẩm mỹ
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (OrderStatus.valueOf(item)) {
                        case PENDING -> setStyle("-fx-text-fill: #fb923c; -fx-font-weight: bold;"); // Cam
                        case CONFIRMED -> setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;"); // Xanh dương
                        case SHIPPED -> setStyle("-fx-text-fill: #06b6d4; -fx-font-weight: bold;"); // Cyan
                        case DELIVERED -> setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;"); // Xanh lá
                        case CANCELLED -> setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); // Đỏ
                    }
                }
            }
        });

        // 2. ComboBox lọc trạng thái
        cbOrderStatusFilter.setItems(FXCollections.observableArrayList(
            "Tất cả", "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"
        ));
        cbOrderStatusFilter.setValue("Tất cả");
        cbOrderStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterOrders(newVal));

        // 3. Sự kiện chọn dòng để kích hoạt nút xem chi tiết
        tblOrders.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            btnViewDetail.setDisable(newSel == null);
        });

        // Click đúp dòng để xem chi tiết
        tblOrders.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Order selected = tblOrders.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openOrderDetail(selected);
                }
            }
        });

        loadOrdersData();
    }

    private void loadOrdersData() {
        if (currentSiteCode == null || currentSiteCode.isEmpty()) {
            siteOrdersList = FXCollections.observableArrayList();
            return;
        }
        siteOrdersList = orderService.getOrdersBySite(currentSiteCode);
        filterOrders(cbOrderStatusFilter.getValue());
    }

    private void filterOrders(String status) {
        if (siteOrdersList == null) return;
        
        if (status == null || status.equals("Tất cả")) {
            tblOrders.setItems(FXCollections.observableArrayList(siteOrdersList));
        } else {
            List<Order> filtered = siteOrdersList.stream()
                .filter(o -> o.getStatus().name().equalsIgnoreCase(status))
                .collect(Collectors.toList());
            tblOrders.setItems(FXCollections.observableArrayList(filtered));
        }
    }

    @FXML
    private void handleRefresh() {
        loadOrdersData();
    }

    @FXML
    private void handleViewDetail() {
        Order selected = tblOrders.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openOrderDetail(selected);
        }
    }

    private void openOrderDetail(Order order) {
        SiteOrderDetailController.selectedOrderId = order.getId();
        NavigationManager.getInstance().navigateTo("/com/nhom18/importorder/view/site/site_order_detail.fxml");
    }
}
