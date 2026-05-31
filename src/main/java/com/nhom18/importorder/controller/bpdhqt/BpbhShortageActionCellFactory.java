package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.ImportRequestItem;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

public class BpbhShortageActionCellFactory implements Callback<TableColumn<ImportRequestItem, Void>, TableCell<ImportRequestItem, Void>> {

    private final CreateFreeRequestController controller;

    public BpbhShortageActionCellFactory(CreateFreeRequestController controller) {
        this.controller = controller;
    }

    @Override
    public TableCell<ImportRequestItem, Void> call(TableColumn<ImportRequestItem, Void> param) {
        return new TableCell<ImportRequestItem, Void>() {
            private final Button btnAdd = new Button();
            {
                btnAdd.setOnAction(event -> {
                    ImportRequestItem rowItem = getTableView().getItems().get(getIndex());
                    handleSelectShortage(rowItem);
                    updateButtonState(true);
                });
            }

            private void handleSelectShortage(ImportRequestItem pendingItem) {
                boolean duplicate = controller.getSelectedItemsData().stream()
                        .anyMatch(item -> item.getId() == pendingItem.getId());
                if (duplicate) return;

                ImportRequestItem newItem = new ImportRequestItem();
                newItem.setId(pendingItem.getId());
                newItem.setRequestId(pendingItem.getRequestId());
                newItem.setMerchandiseCode(pendingItem.getMerchandiseCode());
                newItem.setMerchandiseName(pendingItem.getMerchandiseName());
                int qty = pendingItem.getQuantityShortage() > 0 ? pendingItem.getQuantityShortage() : pendingItem.getQuantityOrdered();
                newItem.setQuantityOrdered(qty);
                newItem.setQuantityShortage(qty);
                newItem.setUnit(pendingItem.getUnit());
                newItem.setDesiredDeliveryDate(pendingItem.getDesiredDeliveryDate());

                controller.getSelectedItemsData().add(newItem);
                controller.getTblSelectedItems().refresh();
                controller.updateTotalQuantityLabel();

                if (controller.getSelectedItemsData().size() == 1) {
                    controller.getDpRequiredDate().setValue(pendingItem.getDesiredDeliveryDate());
                }
            }

            private void updateButtonState(boolean isSelected) {
                if (isSelected) {
                    btnAdd.setText("✓ Thêm");
                    btnAdd.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4px 10px;");
                    btnAdd.setDisable(true);
                } else {
                    btnAdd.setText("➕ Chọn");
                    btnAdd.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4px 10px; -fx-cursor: hand; -fx-border-radius: 4px; -fx-background-radius: 4px;");
                    btnAdd.setDisable(false);
                }
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ImportRequestItem rowItem = getTableView().getItems().get(getIndex());
                    boolean exists = controller.getSelectedItemsData().stream()
                            .anyMatch(selected -> selected.getId() == rowItem.getId());
                    updateButtonState(exists);
                    HBox container = new HBox(btnAdd);
                    container.setStyle("-fx-alignment: CENTER;");
                    setGraphic(container);
                }
            }
        };
    }
}
