package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Merchandise;
import com.nhom18.importorder.model.entity.Order;
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
                tilePane.getChildren().add(CatalogCardRenderer.createCard(controller, m, targetOrder));
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
