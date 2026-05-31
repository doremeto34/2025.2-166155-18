package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class OrderCardItemRenderer {

    public static void render(CreateFreeRequestController controller, Order order, VBox vboxItems, Runnable updateOrderMetrics) {
        vboxItems.getChildren().clear();
        if (order.getItems().isEmpty()) {
            vboxItems.getChildren().add(new Label("Chưa có mặt hàng trong đơn hàng này."));
            return;
        }

        for (int k = 0; k < order.getItems().size(); k++) {
            final int itemIndex = k;
            OrderItem item = order.getItems().get(itemIndex);

            HBox itemRow = new HBox();
            itemRow.setAlignment(Pos.CENTER_LEFT);
            itemRow.setSpacing(10.0);

            double unitPrice = controller.getMerchandisePrices().getOrDefault(item.getMerchandiseCode(), 0.0);
            VBox itemInfo = new VBox(2.0);
            Label lblName = new Label(item.getMerchandiseName());
            lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            
            Label lblPriceDetail = new Label(String.format("Mã: %s  •  Đơn giá: %,.2f USD / %s", 
                item.getMerchandiseCode(), unitPrice, item.getUnit()));
            lblPriceDetail.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 11px;");
            itemInfo.getChildren().addAll(lblName, lblPriceDetail);
            HBox.setHgrow(itemInfo, Priority.ALWAYS);

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
                            controller.resetCheckPassed();
                        }
                    } catch (NumberFormatException ignored) {}
                }
            });

            Button btnDeleteItem = new Button("❌");
            btnDeleteItem.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-font-size: 12px;");
            btnDeleteItem.setOnAction(event -> {
                order.getItems().remove(itemIndex);
                render(controller, order, vboxItems, updateOrderMetrics);
                updateOrderMetrics.run();
                controller.resetCheckPassed();
            });

            itemRow.getChildren().addAll(itemInfo, txtQtyInput, btnDeleteItem);
            vboxItems.getChildren().add(itemRow);
        }
    }
}
