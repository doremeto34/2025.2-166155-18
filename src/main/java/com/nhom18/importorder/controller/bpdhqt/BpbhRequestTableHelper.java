package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.ImportRequest;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.enums.RequestStatus;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class BpbhRequestTableHelper {

    public static void setupRequestColumns(
            TableColumn<ImportRequest, Integer> colId,
            TableColumn<ImportRequest, String> colCreator,
            TableColumn<ImportRequest, String> colDate,
            TableColumn<ImportRequest, RequestStatus> colStatus) {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCreator.setCellValueFactory(new PropertyValueFactory<>("creatorName"));
        colDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedDate().toString()));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(RequestStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null); setText(null);
                } else {
                    Label badge = new Label();
                    badge.getStyleClass().add("badge");
                    switch (item) {
                        case PENDING -> { badge.setText("Chờ Xử Lý"); badge.getStyleClass().add("badge-pending"); }
                        case PROCESSING -> { badge.setText("Đang Xử Lý"); badge.getStyleClass().add("badge-processing"); }
                        case APPROVED -> { badge.setText("Đã Phê Duyệt"); badge.getStyleClass().add("badge-approved"); }
                        case REJECTED -> { badge.setText("Bị Từ Chối"); badge.getStyleClass().add("badge-rejected"); }
                    }
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });
    }

    public static void setupItemColumns(
            TableColumn<ImportRequestItem, String> colItemMerch,
            TableColumn<ImportRequestItem, String> colItemName,
            TableColumn<ImportRequestItem, Integer> colItemQty,
            TableColumn<ImportRequestItem, String> colItemUnit,
            TableColumn<ImportRequestItem, String> colItemDate) {

        colItemMerch.setCellValueFactory(new PropertyValueFactory<>("merchandiseCode"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("merchandiseName"));
        colItemQty.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantityOrdered()).asObject());
        colItemUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colItemDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDesiredDeliveryDate().toString()));
    }
}
