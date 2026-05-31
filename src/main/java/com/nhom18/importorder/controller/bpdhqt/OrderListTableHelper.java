package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.enums.OrderStatus;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

public class OrderListTableHelper {

    public static void setupOrderColumns(
            TableColumn<Order, Integer> colOrderId,
            TableColumn<Order, String> colSiteName,
            TableColumn<Order, String> colDeliveryMethod,
            TableColumn<Order, String> colCreatedDate,
            TableColumn<Order, String> colEstArrival,
            TableColumn<Order, String> colStatus) {

        colOrderId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        colSiteName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSiteName()));
        colDeliveryMethod.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDeliveryMethod().name()));
        colCreatedDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedDate().toString()));
        colEstArrival.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstimatedArrival().toString()));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));

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
                        case PENDING -> setStyle("-fx-text-fill: #fb923c; -fx-font-weight: bold;");
                        case CONFIRMED -> setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                        case SHIPPED -> setStyle("-fx-text-fill: #06b6d4; -fx-font-weight: bold;");
                        case DELIVERED -> setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                        case CANCELLED -> setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    public static void setupItemColumns(
            TableColumn<OrderItem, String> colItemCode,
            TableColumn<OrderItem, String> colItemName,
            TableColumn<OrderItem, Integer> colItemQtyOrdered,
            TableColumn<OrderItem, Integer> colItemQtyConfirmed,
            TableColumn<OrderItem, Integer> colItemQtyReceived,
            TableColumn<OrderItem, String> colItemUnit) {

        colItemCode.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchandiseCode()));
        colItemName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchandiseName()));
        colItemQtyOrdered.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantityOrdered()).asObject());
        colItemQtyConfirmed.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantityConfirmed()).asObject());
        colItemQtyReceived.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantityReceived()).asObject());
        colItemUnit.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUnit()));
    }
}
