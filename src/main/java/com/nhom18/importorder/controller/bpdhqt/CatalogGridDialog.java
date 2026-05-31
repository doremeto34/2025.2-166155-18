package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Merchandise;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.util.AlertHelper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.*;

public class CatalogGridDialog {

    public static void show(CreateFreeRequestController controller, Order targetOrder) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(targetOrder == null ? "Catalog Mặt Hàng" : "Thêm Mặt Hàng vào Đơn Hàng");
        
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double dialogWidth = Math.min(840, screenBounds.getWidth() * 0.9);
        double dialogHeight = Math.min(600, screenBounds.getHeight() * 0.85);
        
        dialogStage.setMinWidth(600);
        dialogStage.setMinHeight(450);
        dialogStage.setWidth(dialogWidth);
        dialogStage.setHeight(dialogHeight);

        VBox root = new VBox(15);
        root.setStyle("-fx-padding: 20px; -fx-background-color: #f8fafc;");

        Label title = new Label(targetOrder == null ? "Danh Mục Mặt Hàng Catalog" : "Thêm Mặt Hàng vào Đơn Hàng");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: -text-primary;");

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("🔍 Tìm kiếm theo tên hoặc mã mặt hàng...");
        txtSearch.setPrefWidth(350);
        txtSearch.setStyle("-fx-background-radius: 6px; -fx-padding: 8px 12px; -fx-border-color: #cbd5e1;");

        HBox header = new HBox(15, title, txtSearch);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);
        root.getChildren().add(header);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        TilePane tilePane = new TilePane();
        tilePane.setHgap(15);
        tilePane.setVgap(15);
        tilePane.setPadding(new Insets(5));
        tilePane.setPrefColumns(3); 
        scroll.setContent(tilePane);
        root.getChildren().add(scroll);

        Runnable renderGrid = () -> {
            tilePane.getChildren().clear();
            String query = txtSearch.getText().toLowerCase().trim();

            for (Merchandise m : controller.getActiveMerchandise()) {
                if (!query.isEmpty() && !m.getName().toLowerCase().contains(query) && !m.getMerchandiseCode().toLowerCase().contains(query)) {
                    continue; 
                }

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
                                    newItem.setDesiredDeliveryDate(controller.getDpRequiredDate().getValue() != null ? controller.getDpRequiredDate().getValue() : LocalDate.now().plusDays(10));
                                    controller.getSelectedItemsData().add(newItem);
                                }

                                controller.getTblSelectedItems().refresh();
                                controller.updateTotalQuantityLabel();
                            } else {
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
                    } catch (NumberFormatException ignored) {
                        AlertHelper.showWarning("Lỗi", "Số lượng phải là số nguyên dương.");
                    }
                });

                HBox actionBox = new HBox(6, new Label("SL:"), qtyInput, btnAdd);
                actionBox.setAlignment(Pos.CENTER);
                
                card.getChildren().addAll(name, code, price, actionBox);
                tilePane.getChildren().add(card);
            }
        };

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> renderGrid.run());
        renderGrid.run();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(controller.getClass().getResource("/css/styles.css").toExternalForm());
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }
}
