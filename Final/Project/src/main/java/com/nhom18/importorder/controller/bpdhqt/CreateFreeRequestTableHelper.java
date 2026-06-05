package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.ImportRequestItem;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

public class CreateFreeRequestTableHelper {

    public static void setupColumns(
            TableView<ImportRequestItem> tblSelectedItems,
            TableColumn<ImportRequestItem, String> colItemName,
            TableColumn<ImportRequestItem, String> colItemCode,
            TableColumn<ImportRequestItem, Integer> colItemQty,
            TableColumn<ImportRequestItem, String> colItemUnit,
            TableColumn<ImportRequestItem, Void> colItemAction,
            ObservableList<ImportRequestItem> selectedItemsData,
            CreateFreeRequestController controller) {

        colItemName.setCellValueFactory(new PropertyValueFactory<>("merchandiseName"));
        colItemCode.setCellValueFactory(new PropertyValueFactory<>("merchandiseCode"));
        colItemQty.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantityOrdered()).asObject());
        colItemUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

        colItemAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("❌");
            {
                btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-cursor: hand;");
                btnDelete.setOnAction(evt -> {
                    selectedItemsData.remove(getTableView().getItems().get(getIndex()));
                    controller.updateTotalQuantityLabel();
                });
            }
            @Override
            protected void updateItem(Void it, boolean empty) {
                super.updateItem(it, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(btnDelete);
                    box.setStyle("-fx-alignment: CENTER;");
                    setGraphic(box);
                }
            }
        });

        tblSelectedItems.setItems(selectedItemsData);
    }
}
