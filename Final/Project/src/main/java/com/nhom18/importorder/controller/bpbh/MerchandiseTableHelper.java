package com.nhom18.importorder.controller.bpbh;

import com.nhom18.importorder.model.entity.Merchandise;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class MerchandiseTableHelper {

    public static void setupColumns(
            TableColumn<Merchandise, String> colCode,
            TableColumn<Merchandise, String> colName,
            TableColumn<Merchandise, String> colDescription,
            TableColumn<Merchandise, String> colUnit,
            TableColumn<Merchandise, Double> colPrice,
            TableColumn<Merchandise, String> colStatus) {

        colCode.setCellValueFactory(new PropertyValueFactory<>("merchandiseCode"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isActive() ? "Đang kinh doanh" : "Ngừng kinh doanh"));

        colPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText((empty || price == null) ? null : String.format("$%,.2f", price));
            }
        });

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(item);
                    if (item.equals("Đang kinh doanh")) {
                        setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }
}
