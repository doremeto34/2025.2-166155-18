package com.nhom18.importorder.controller.bpqlk;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;

public class WarehouseReceiptTableHelper {

    public static void setupColumns(
            TableColumn<ReconciliationRow, Boolean> colChecked,
            TableColumn<ReconciliationRow, String> colMerchCode,
            TableColumn<ReconciliationRow, String> colMerchName,
            TableColumn<ReconciliationRow, Integer> colQtyOrdered,
            TableColumn<ReconciliationRow, Integer> colQtyConfirmed,
            TableColumn<ReconciliationRow, Integer> colQtyReceived,
            TableColumn<ReconciliationRow, Integer> colDiscrepancy,
            TableColumn<ReconciliationRow, String> colDiscrepancyNotes) {

        colMerchCode.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchCode()));
        colMerchName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchName()));
        colQtyOrdered.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQtyOrdered()).asObject());
        colQtyConfirmed.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQtyConfirmed()).asObject());
        colDiscrepancy.setCellValueFactory(cell -> cell.getValue().discrepancyProperty().asObject());

        setupCheckedColumn(colChecked);
        setupQtyReceivedColumn(colQtyReceived);
        setupDiscrepancyColumn(colDiscrepancy);
        setupDiscrepancyNotesColumn(colDiscrepancyNotes);
    }

    private static void setupCheckedColumn(TableColumn<ReconciliationRow, Boolean> colChecked) {
        colChecked.setCellValueFactory(cell -> cell.getValue().checkedProperty());
        colChecked.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    ReconciliationRow row = getTableRow().getItem();
                    if (row != null) {
                        row.setChecked(checkBox.isSelected());
                    }
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ReconciliationRow row = getTableRow().getItem();
                    if (row != null) {
                        checkBox.setSelected(row.isChecked());
                    }
                    setGraphic(checkBox);
                }
            }
        });
    }

    private static void setupQtyReceivedColumn(TableColumn<ReconciliationRow, Integer> colQtyReceived) {
        colQtyReceived.setCellValueFactory(cell -> cell.getValue().qtyReceivedProperty().asObject());
        colQtyReceived.setCellFactory(column -> new TableCell<>() {
            private final TextField txtQty = new TextField();
            private boolean isUpdating = false;
            {
                txtQty.setPrefWidth(80);
                txtQty.setStyle("-fx-alignment: CENTER;");
                txtQty.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (isUpdating) return;
                    ReconciliationRow row = getTableRow().getItem();
                    if (row != null && newVal != null && !newVal.trim().isEmpty()) {
                        try {
                            int val = Integer.parseInt(newVal.trim());
                            if (val >= 0) {
                                row.setQtyReceived(val);
                            }
                        } catch (NumberFormatException e) {
                            // Bỏ qua giá trị nhập không hợp lệ
                        }
                    }
                });
            }
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ReconciliationRow row = getTableRow().getItem();
                    if (row != null) {
                        isUpdating = true;
                        txtQty.setText(String.valueOf(row.getQtyReceived()));
                        isUpdating = false;
                    }
                    setGraphic(txtQty);
                }
            }
        });
    }

    private static void setupDiscrepancyColumn(TableColumn<ReconciliationRow, Integer> colDiscrepancy) {
        colDiscrepancy.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item > 0 ? "+" + item : String.valueOf(item));
                    if (item < 0) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); // Đỏ (Hụt hàng)
                    } else if (item > 0) {
                        setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;"); // Xanh lá (Dư hàng)
                    } else {
                        setStyle("-fx-text-fill: -text-secondary;"); // 0 (Đủ hàng)
                    }
                }
            }
        });
    }

    private static void setupDiscrepancyNotesColumn(TableColumn<ReconciliationRow, String> colDiscrepancyNotes) {
        colDiscrepancyNotes.setCellValueFactory(cell -> cell.getValue().notesProperty());
        colDiscrepancyNotes.setCellFactory(column -> new TableCell<>() {
            private final TextField txtNote = new TextField();
            private boolean isUpdating = false;
            {
                txtNote.setPromptText("Nhập lý do nếu lệch...");
                txtNote.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (isUpdating) return;
                    ReconciliationRow row = getTableRow().getItem();
                    if (row != null) {
                        row.setNotes(newVal);
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ReconciliationRow row = getTableRow().getItem();
                    if (row != null) {
                        isUpdating = true;
                        txtNote.setText(row.getNotes() != null ? row.getNotes() : "");
                        isUpdating = false;
                    }
                    setGraphic(txtNote);
                }
            }
        });
    }
}
