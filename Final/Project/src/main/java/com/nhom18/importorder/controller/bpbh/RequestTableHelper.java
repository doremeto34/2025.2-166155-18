package com.nhom18.importorder.controller.bpbh;

import com.nhom18.importorder.model.entity.ImportRequest;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.entity.Merchandise;
import java.time.LocalDate;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

public class RequestTableHelper {

    public static void setupCatalogColumns(
            TableColumn<Merchandise, String> colCatalogCode,
            TableColumn<Merchandise, String> colCatalogName,
            TableColumn<Merchandise, String> colCatalogUnit) {
        colCatalogCode.setCellValueFactory(new PropertyValueFactory<>("merchandiseCode"));
        colCatalogName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCatalogUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
    }

    public static void setupCartColumns(
            TableColumn<ImportRequestItem, String> colCartCode,
            TableColumn<ImportRequestItem, String> colCartName,
            TableColumn<ImportRequestItem, Integer> colCartQty,
            TableColumn<ImportRequestItem, String> colCartUnit,
            TableColumn<ImportRequestItem, LocalDate> colCartDate,
            TableColumn<ImportRequestItem, Void> colCartAction,
            ObservableList<ImportRequestItem> cartData) {

        colCartCode.setCellValueFactory(new PropertyValueFactory<>("merchandiseCode"));
        colCartName.setCellValueFactory(new PropertyValueFactory<>("merchandiseName"));
        colCartQty.setCellValueFactory(new PropertyValueFactory<>("quantityOrdered"));
        colCartUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colCartDate.setCellValueFactory(new PropertyValueFactory<>("desiredDeliveryDate"));

        colCartAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("❌");
            {
                btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-font-weight: bold;");
                btnDelete.setOnAction(event -> {
                    ImportRequestItem item = getTableView().getItems().get(getIndex());
                    cartData.remove(item);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    HBox container = new HBox(btnDelete);
                    container.setStyle("-fx-alignment: CENTER;");
                    setGraphic(container);
                }
            }
        });
    }

    public static void setupRequestListColumns(
            TableColumn<ImportRequest, Integer> colId,
            TableColumn<ImportRequest, String> colCreator,
            TableColumn<ImportRequest, String> colDate,
            TableColumn<ImportRequest, String> colStatus) {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCreator.setCellValueFactory(new PropertyValueFactory<>("creatorName"));
        colDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedDate().toString()));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(
            switch (cell.getValue().getStatus()) {
                case PENDING -> "Chờ Xử Lý";
                case PROCESSING -> "Đang Xử Lý";
                case APPROVED -> "Đã Phê Duyệt";
                case REJECTED -> "Bị Từ Chối";
            }
        ));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Chờ Xử Lý" -> setStyle("-fx-text-fill: #fb923c; -fx-font-weight: bold;");
                        case "Đang Xử Lý" -> setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                        case "Đã Phê Duyệt" -> setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                        case "Bị Từ Chối" -> setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }
}
