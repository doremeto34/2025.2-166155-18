package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Merchandise;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.util.AlertHelper;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.util.Timer;
import java.util.TimerTask;

public class CatalogCardRenderer {

    public static VBox createCard(CreateFreeRequestController controller, Merchandise m, Order targetOrder) {
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
                        addToLeftList(controller, m, q);
                    } else {
                        addToRightOrder(controller, targetOrder, m, q);
                    }
                    showSuccessEffect(btnAdd);
                }
            } catch (NumberFormatException ignored) {
                AlertHelper.showWarning("Lỗi", "Số lượng phải là số nguyên dương.");
            }
        });

        HBox actionBox = new HBox(6, new Label("SL:"), qtyInput, btnAdd);
        actionBox.setAlignment(Pos.CENTER);
        card.getChildren().addAll(name, code, price, actionBox);
        return card;
    }

    private static void addToLeftList(CreateFreeRequestController controller, Merchandise m, int q) {
        boolean duplicate = false;
        for (ImportRequestItem item : controller.getSelectedItemsData()) {
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
            newItem.setDesiredDeliveryDate(controller.getDpRequiredDate().getValue() != null 
                ? controller.getDpRequiredDate().getValue() : LocalDate.now().plusDays(10));
            controller.getSelectedItemsData().add(newItem);
        }
        controller.getTblSelectedItems().refresh();
        controller.updateTotalQuantityLabel();
    }

    private static void addToRightOrder(CreateFreeRequestController controller, Order targetOrder, Merchandise m, int q) {
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
        controller.renderOrderCards();
        controller.resetCheckPassed();
    }

    private static void showSuccessEffect(Button btnAdd) {
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
}
