package com.nhom18.importorder.controller.site;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.service.OrderService;
import com.nhom18.importorder.util.NavigationManager;
import com.nhom18.importorder.util.SessionManager;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
        if (SessionManager.getInstance().getCurrentUser() != null) {
            currentSiteCode = SessionManager.getInstance().getCurrentUser().getSiteCode();
        }

        SiteOrderListTableHelper.setupColumns(
            colOrderId, colReqId, colDeliveryMethod, colCreatedDate, colEstArrival, colStatus
        );

        cbOrderStatusFilter.setItems(FXCollections.observableArrayList(
            "Tất cả", "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"
        ));
        cbOrderStatusFilter.setValue("Tất cả");
        cbOrderStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterOrders(newVal));

        tblOrders.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            btnViewDetail.setDisable(newSel == null);
        });

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
            tblOrders.setItems(FXCollections.observableArrayList(
                siteOrdersList.stream()
                    .filter(o -> o.getStatus().name().equalsIgnoreCase(status))
                    .collect(Collectors.toList())
            ));
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
