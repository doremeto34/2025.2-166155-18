package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.dao.IImportRequestDAO;
import com.nhom18.importorder.dao.impl.SQLiteImportRequestDAO;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.util.List;

public class BpbhShortagesDialog {

    public static void show(CreateFreeRequestController controller) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Danh Sách Yêu Cầu Mặt Hàng BPBH (Shortages)");

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double dialogWidth = Math.min(840, screenBounds.getWidth() * 0.9);
        double dialogHeight = Math.min(550, screenBounds.getHeight() * 0.8);
        
        dialogStage.setMinWidth(600);
        dialogStage.setMinHeight(400);
        dialogStage.setWidth(dialogWidth);
        dialogStage.setHeight(dialogHeight);

        VBox root = new VBox(15);
        root.setStyle("-fx-padding: 20px; -fx-background-color: #f8fafc;");

        Label title = new Label("Danh Sách Mặt Hàng BPBH Cần Đặt Hàng Quốc Tế");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: -text-primary;");
        
        Label subtitle = new Label("Đây là lượng thiếu hụt (Shortages) sau khi đã khấu trừ Tồn Kho Nội Bộ tự động.");
        subtitle.setStyle("-fx-text-fill: -text-secondary; -fx-font-style: italic; -fx-font-size: 12px;");

        root.getChildren().addAll(title, subtitle);

        TableView<ImportRequestItem> table = new TableView<>();
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<ImportRequestItem, Integer> colReqId = new TableColumn<>("Mã Yêu Cầu");
        colReqId.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        colReqId.setPrefWidth(100);

        TableColumn<ImportRequestItem, String> colCode = new TableColumn<>("Mã SP");
        colCode.setCellValueFactory(new PropertyValueFactory<>("merchandiseCode"));
        colCode.setPrefWidth(100);

        TableColumn<ImportRequestItem, String> colName = new TableColumn<>("Tên Sản Phẩm");
        colName.setCellValueFactory(new PropertyValueFactory<>("merchandiseName"));
        colName.setPrefWidth(220);

        TableColumn<ImportRequestItem, Integer> colShortage = new TableColumn<>("S.Lượng Thiếu");
        colShortage.setCellValueFactory(cell -> {
            if (cell.getValue() == null) {
                return new SimpleIntegerProperty(0).asObject();
            }
            int dispQty = cell.getValue().getQuantityShortage() > 0 ? cell.getValue().getQuantityShortage() : cell.getValue().getQuantityOrdered();
            return new SimpleIntegerProperty(dispQty).asObject();
        });
        colShortage.setPrefWidth(120);

        TableColumn<ImportRequestItem, String> colUnit = new TableColumn<>("Đơn Vị");
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colUnit.setPrefWidth(80);

        TableColumn<ImportRequestItem, String> colDate = new TableColumn<>("Hạn Giao");
        colDate.setCellValueFactory(cell -> {
            if (cell.getValue() == null || cell.getValue().getDesiredDeliveryDate() == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(cell.getValue().getDesiredDeliveryDate().toString());
        });
        colDate.setPrefWidth(120);

        table.getColumns().addAll(colReqId, colCode, colName, colShortage, colUnit, colDate);

        IImportRequestDAO importRequestDAO = new SQLiteImportRequestDAO();
        List<ImportRequestItem> pendingItems = importRequestDAO.getPendingRequestItems();
        table.setItems(FXCollections.observableArrayList(pendingItems));

        TableColumn<ImportRequestItem, Void> colAction = new TableColumn<>("Hành Động");
        colAction.setPrefWidth(100);
        colAction.setCellFactory(new BpbhShortageActionCellFactory(controller));
        table.getColumns().add(colAction);

        Button btnClose = new Button("Đóng");
        btnClose.getStyleClass().add("button-secondary");
        btnClose.setStyle("-fx-padding: 8px 18px;");
        btnClose.setOnAction(e -> dialogStage.close());

        HBox footer = new HBox(btnClose);
        footer.setStyle("-fx-alignment: CENTER_RIGHT;");

        root.getChildren().addAll(table, footer);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(controller.getClass().getResource("/css/styles.css").toExternalForm());
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }
}
