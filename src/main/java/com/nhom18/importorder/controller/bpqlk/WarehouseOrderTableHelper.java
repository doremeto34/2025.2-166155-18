package com.nhom18.importorder.controller.bpqlk;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.enums.OrderStatus;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

public class WarehouseOrderTableHelper {

    public static void setupColumns(
            TableColumn<Order, Integer> colOrderId,
            TableColumn<Order, Integer> colReqId,
            TableColumn<Order, String> colSiteName,
            TableColumn<Order, String> colDeliveryMethod,
            TableColumn<Order, String> colEstArrival,
            TableColumn<Order, String> colStatus) {

        colOrderId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        colReqId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getRequestId()).asObject());
        colSiteName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSiteName()));
        colDeliveryMethod.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDeliveryMethod().name()));
        colEstArrival.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstimatedArrival().toString()));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    switch (OrderStatus.valueOf(item)) {
                        case PENDING -> { setText("Chờ nhập kho (PENDING)"); setStyle("-fx-text-fill: #fb923c; -fx-font-weight: bold;"); }
                        case CONFIRMED -> { setText("Chờ nhập kho (CONFIRMED)"); setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;"); }
                        case SHIPPED -> { setText("Chờ nhập kho (SHIPPED)"); setStyle("-fx-text-fill: #06b6d4; -fx-font-weight: bold;"); }
                        case DELIVERED -> { setText("Đã nhập kho (DELIVERED)"); setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;"); }
                        case CANCELLED -> { setText("Đã hủy (CANCELLED)"); setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); }
                    }
                }
            }
        });
    }

    public static List<Order> filter(List<Order> allOrders, String selectedFilter, String searchKeyword) {
        if (allOrders == null) return List.of();
        String kw = searchKeyword.trim().toLowerCase();
        return allOrders.stream()
            .filter(o -> {
                if ("Chờ nhập kho".equals(selectedFilter)) {
                    return o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.CONFIRMED || o.getStatus() == OrderStatus.SHIPPED;
                } else if ("Đã nhập kho".equals(selectedFilter)) {
                    return o.getStatus() == OrderStatus.DELIVERED;
                } else if ("Đã hủy".equals(selectedFilter)) {
                    return o.getStatus() == OrderStatus.CANCELLED;
                }
                return true;
            })
            .filter(o -> {
                if (kw.isEmpty()) return true;
                boolean matchOrderId = String.valueOf(o.getId()).contains(kw);
                boolean matchSiteName = o.getSiteName().toLowerCase().contains(kw);
                boolean matchProductCode = o.getItems().stream()
                    .anyMatch(item -> item.getMerchandiseCode().toLowerCase().contains(kw));
                return matchOrderId || matchSiteName || matchProductCode;
            })
            .collect(Collectors.toList());
    }
}
