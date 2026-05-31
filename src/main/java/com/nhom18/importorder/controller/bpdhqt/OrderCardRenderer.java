package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.model.enums.DeliveryMethod;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import java.time.LocalDate;

public class OrderCardRenderer {

    public static void render(CreateFreeRequestController controller) {
        controller.getVboxOrdersContainer().getChildren().clear();
        controller.getLblOrdersCount().setText("Danh Sách Đơn Hàng Đề Xuất (" + controller.getProposedOrdersData().size() + ")");

        for (int i = 0; i < controller.getProposedOrdersData().size(); i++) {
            final int index = i;
            Order order = controller.getProposedOrdersData().get(index);
            
            VBox card = new VBox();
            card.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-padding: 15px; -fx-spacing: 12px;");
            
            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            Label lblOrderNo = new Label("Đơn Đặt Hàng #" + (index + 1));
            lblOrderNo.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: -text-primary;");
            HBox.setHgrow(lblOrderNo, Priority.ALWAYS);

            Button btnDeleteOrder = new Button("🗑️ Delete");
            btnDeleteOrder.getStyleClass().addAll("button-secondary");
            btnDeleteOrder.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-padding: 4px 10px;");
            btnDeleteOrder.setOnAction(event -> {
                controller.getProposedOrdersData().remove(index);
                controller.renderOrderCards();
                controller.resetCheckPassed();
            });
            header.getChildren().addAll(lblOrderNo, btnDeleteOrder);
            card.getChildren().add(header);

            GridPane formGrid = new GridPane();
            formGrid.setHgap(15);
            formGrid.setVgap(10);

            Label lblSite = new Label("Site đối tác:");
            lblSite.setStyle("-fx-font-weight: bold; -fx-text-fill: -text-secondary; -fx-font-size: 12px;");
            formGrid.add(lblSite, 0, 0);

            ComboBox<Site> cbSite = new ComboBox<>(FXCollections.observableArrayList(controller.getActiveSites()));
            cbSite.setPrefWidth(280.0);
            for (Site s : controller.getActiveSites()) {
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
                public Site fromString(String string) { return null; }
            });
            formGrid.add(cbSite, 1, 0);

            Label lblMethod = new Label("Vận chuyển:");
            lblMethod.setStyle("-fx-font-weight: bold; -fx-text-fill: -text-secondary; -fx-font-size: 12px;");
            formGrid.add(lblMethod, 0, 1);

            ComboBox<String> cbMethod = new ComboBox<>();
            cbMethod.setPrefWidth(280.0);
            formGrid.add(cbMethod, 1, 1);
            card.getChildren().add(formGrid);

            Label lblSummary = new Label();
            lblSummary.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 13px;");
            card.getChildren().add(lblSummary);

            VBox vboxItems = new VBox();
            vboxItems.setSpacing(8.0);
            vboxItems.setStyle("-fx-background-color: rgba(248,250,252,0.6); -fx-background-radius: 6px; -fx-padding: 10px;");
            
            Runnable updateOrderMetrics = () -> {
                Site selectedSite = cbSite.getValue();
                if (selectedSite != null) {
                    order.setSiteCode(selectedSite.getSiteCode());
                    order.setSiteName(selectedSite.getName());

                    cbMethod.setItems(FXCollections.observableArrayList(
                        "Đường biển (SHIP - " + selectedSite.getShipDays() + " ngày)",
                        "Đường hàng không (AIR - " + selectedSite.getAirDays() + " ngày)"
                    ));

                    if (order.getDeliveryMethod() == DeliveryMethod.SHIP) {
                        cbMethod.setValue("Đường biển (SHIP - " + selectedSite.getShipDays() + " ngày)");
                        order.setEstimatedArrival(LocalDate.now().plusDays(selectedSite.getShipDays()));
                    } else {
                        cbMethod.setValue("Đường hàng không (AIR - " + selectedSite.getAirDays() + " ngày)");
                        order.setEstimatedArrival(LocalDate.now().plusDays(selectedSite.getAirDays()));
                    }
                }

                double totalVal = 0;
                int itemsCount = 0;
                for (OrderItem oi : order.getItems()) {
                    double pr = controller.getMerchandisePrices().getOrDefault(oi.getMerchandiseCode(), 0.0);
                    totalVal += pr * oi.getQuantityOrdered();
                    itemsCount += oi.getQuantityOrdered();
                }

                int days = (order.getDeliveryMethod() == DeliveryMethod.SHIP && selectedSite != null) ? selectedSite.getShipDays() : (selectedSite != null ? selectedSite.getAirDays() : 0);
                lblSummary.setText(String.format("Tổng tiền: %,.2f USD  •  %d sản phẩm  •  Giao hàng: %d ngày (Dự kiến đến: %s)", 
                    totalVal, itemsCount, days, order.getEstimatedArrival().toString()));
            };

            cbSite.valueProperty().addListener((obs, oldSite, newSite) -> {
                if (newSite != null) {
                    updateOrderMetrics.run();
                    if (oldSite != null) {
                        controller.resetCheckPassed();
                    }
                }
            });

            cbMethod.valueProperty().addListener((obs, oldMethod, newMethod) -> {
                if (newMethod != null) {
                    if (newMethod.contains("SHIP")) {
                        order.setDeliveryMethod(DeliveryMethod.SHIP);
                    } else {
                        order.setDeliveryMethod(DeliveryMethod.AIR);
                    }
                    updateOrderMetrics.run();
                    if (oldMethod != null) {
                        controller.resetCheckPassed();
                    }
                }
            });

            updateOrderMetrics.run();
            OrderCardItemRenderer.render(controller, order, vboxItems, updateOrderMetrics);
            card.getChildren().add(vboxItems);

            Button btnAddItemToOrder = new Button("➕ Add Item from Catalog");
            btnAddItemToOrder.getStyleClass().addAll("button-secondary");
            btnAddItemToOrder.setStyle("-fx-font-size: 12px; -fx-padding: 4px 12px; -fx-background-color: transparent; -fx-border-color: #cbd5e1; -fx-border-radius: 4px;");
            btnAddItemToOrder.setOnAction(event -> controller.openCatalogGrid(order));

            card.getChildren().add(btnAddItemToOrder);
            controller.getVboxOrdersContainer().getChildren().add(card);
        }
    }
}
