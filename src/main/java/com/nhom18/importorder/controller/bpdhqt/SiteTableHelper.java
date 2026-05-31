package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.model.enums.OrderStatus;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

public class SiteTableHelper {

    public static void setupSiteColumns(
            TableColumn<Site, String> colSiteCode,
            TableColumn<Site, String> colName,
            TableColumn<Site, Integer> colShipDays,
            TableColumn<Site, Integer> colAirDays,
            TableColumn<Site, String> colStatus) {

        colSiteCode.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSiteCode()));
        colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        colShipDays.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getShipDays()).asObject());
        colAirDays.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getAirDays()).asObject());
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isActive() ? "Đang hoạt động" : "Ngừng hoạt động"));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                    getStyleClass().removeAll("badge", "badge-success", "badge-rejected");
                } else {
                    setText(item); getStyleClass().add("badge");
                    if (item.equals("Đang hoạt động")) {
                        getStyleClass().removeAll("badge-rejected"); getStyleClass().add("badge-success");
                        setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                    } else {
                        getStyleClass().removeAll("badge-success"); getStyleClass().add("badge-rejected");
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    public static void setupHistoryColumns(
            TableColumn<Order, Integer> colOrderId,
            TableColumn<Order, Integer> colReqId,
            TableColumn<Order, String> colDeliveryMethod,
            TableColumn<Order, String> colCreatedDate,
            TableColumn<Order, String> colEstArrival,
            TableColumn<Order, String> colOrderStatus) {

        colOrderId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        colReqId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getRequestId()).asObject());
        colDeliveryMethod.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDeliveryMethod().name()));
        colCreatedDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedDate().toString()));
        colEstArrival.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstimatedArrival().toString()));
        colOrderStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));

        colOrderStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
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

    public static List<Site> filter(List<Site> allSites, String keyword, String statusFilter) {
        if (allSites == null) return List.of();
        String kw = keyword.toLowerCase().trim();
        return allSites.stream()
            .filter(site -> {
                boolean matchesKeyword = kw.isEmpty() ||
                    site.getSiteCode().toLowerCase().contains(kw) ||
                    site.getName().toLowerCase().contains(kw);
                boolean matchesStatus = true;
                if ("Đang hoạt động".equals(statusFilter)) matchesStatus = site.isActive();
                else if ("Ngừng hoạt động".equals(statusFilter)) matchesStatus = !site.isActive();
                return matchesKeyword && matchesStatus;
            })
            .collect(Collectors.toList());
    }
}
