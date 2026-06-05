package com.nhom18.importorder.controller.site;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.enums.OrderStatus;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

public class SiteOrderListTableHelper {

    public static void setupColumns(
            TableColumn<Order, Integer> colOrderId,
            TableColumn<Order, Integer> colReqId,
            TableColumn<Order, String> colDeliveryMethod,
            TableColumn<Order, String> colCreatedDate,
            TableColumn<Order, String> colEstArrival,
            TableColumn<Order, String> colStatus) {

        colOrderId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        colReqId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getRequestId()).asObject());
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
                        case PENDING -> setStyle("-fx-text-fill: #fb923c; -fx-font-weight: bold;"); // Cam
                        case CONFIRMED -> setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;"); // Xanh dương
                        case SHIPPED -> setStyle("-fx-text-fill: #06b6d4; -fx-font-weight: bold;"); // Cyan
                        case DELIVERED -> setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;"); // Xanh lá
                        case CANCELLED -> setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); // Đỏ
                    }
                }
            }
        });
    }
}
