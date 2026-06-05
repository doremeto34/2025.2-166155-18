package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Order;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

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

            VBox vboxItems = new VBox();
            vboxItems.setSpacing(8.0);
            vboxItems.setStyle("-fx-background-color: rgba(248,250,252,0.6); -fx-background-radius: 6px; -fx-padding: 10px;");
            
            OrderCardFormSetup.setup(controller, order, card, vboxItems);

            Button btnAddItemToOrder = new Button("➕ Add Item from Catalog");
            btnAddItemToOrder.getStyleClass().addAll("button-secondary");
            btnAddItemToOrder.setStyle("-fx-font-size: 12px; -fx-padding: 4px 12px; -fx-background-color: transparent; -fx-border-color: #cbd5e1; -fx-border-radius: 4px;");
            btnAddItemToOrder.setOnAction(event -> controller.openCatalogGrid(order));

            card.getChildren().add(btnAddItemToOrder);
            controller.getVboxOrdersContainer().getChildren().add(card);
        }
    }
}
