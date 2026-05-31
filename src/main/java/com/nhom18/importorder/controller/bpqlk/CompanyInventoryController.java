package com.nhom18.importorder.controller.bpqlk;

import com.nhom18.importorder.dao.ICompanyInventoryDAO;
import com.nhom18.importorder.dao.impl.SQLiteCompanyInventoryDAO;
import com.nhom18.importorder.model.entity.CompanyInventory;
import com.nhom18.importorder.util.AlertHelper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import java.util.List;

public class CompanyInventoryController {

    @FXML
    private Label lblMerchandiseCode;
    @FXML
    private TextField txtNewQuantity;
    @FXML
    private Button btnUpdate;

    @FXML
    private TableView<CompanyInventory> tblInventory;
    @FXML
    private TableColumn<CompanyInventory, String> colMerchandiseCode;
    @FXML
    private TableColumn<CompanyInventory, String> colMerchandiseName;
    @FXML
    private TableColumn<CompanyInventory, Integer> colInStockQuantity;
    @FXML
    private TableColumn<CompanyInventory, String> colUnit;

    private final ICompanyInventoryDAO companyInventoryDAO;

    public CompanyInventoryController() {
        this.companyInventoryDAO = new SQLiteCompanyInventoryDAO();
    }

    @FXML
    public void initialize() {
        // 1. Ánh xạ dữ liệu cột TableView (Thêm kiểm tra null-safe)
        colMerchandiseCode.setCellValueFactory(cell -> {
            if (cell.getValue() == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(cell.getValue().getMerchandiseCode());
        });
        colMerchandiseName.setCellValueFactory(cell -> {
            if (cell.getValue() == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(cell.getValue().getMerchandiseName());
        });
        colInStockQuantity.setCellValueFactory(cell -> {
            if (cell.getValue() == null) return new SimpleIntegerProperty(0).asObject();
            return new SimpleIntegerProperty(cell.getValue().getInStockQuantity()).asObject();
        });
        colUnit.setCellValueFactory(cell -> {
            if (cell.getValue() == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(cell.getValue().getUnit());
        });

        // 2. Lắng nghe chọn dòng trong bảng để nạp vào Form chỉnh sửa nhanh
        tblInventory.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                lblMerchandiseCode.setText(newSel.getMerchandiseCode());
                txtNewQuantity.setText(String.valueOf(newSel.getInStockQuantity()));
                btnUpdate.setDisable(false);
            } else {
                clearEditForm();
            }
        });

        loadInventoryData();
    }

    private void loadInventoryData() {
        List<CompanyInventory> list = companyInventoryDAO.getAll();
        tblInventory.setItems(FXCollections.observableArrayList(list));
    }

    private void clearEditForm() {
        lblMerchandiseCode.setText("[Vui lòng chọn dòng bên dưới]");
        txtNewQuantity.clear();
        btnUpdate.setDisable(true);
    }

    @FXML
    private void handleRefresh() {
        loadInventoryData();
        clearEditForm();
    }

    @FXML
    private void handleUpdateStock() {
        CompanyInventory selected = tblInventory.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("Lỗi", "Vui lòng chọn mặt hàng để cập nhật tồn kho!");
            return;
        }

        String inputQty = txtNewQuantity.getText().trim();
        if (inputQty.isEmpty()) {
            AlertHelper.showWarning("Cảnh báo", "Vui lòng nhập số lượng tồn kho mới!");
            return;
        }

        int newQty;
        try {
            newQty = Integer.parseInt(inputQty);
            if (newQty < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            AlertHelper.showError("Lỗi nhập liệu", "Số lượng tồn kho mới phải là số nguyên không âm (>= 0)!");
            return;
        }

        try {
            companyInventoryDAO.updateStock(selected.getMerchandiseCode(), newQty);
            AlertHelper.showInfo("Thành công", "Đã cập nhật tồn kho mặt hàng " + selected.getMerchandiseCode() + " thành công!");
            loadInventoryData();
            clearEditForm();
        } catch (Exception e) {
            AlertHelper.showError("Lỗi Cập Nhật", "Không thể cập nhật tồn kho:\n" + e.getMessage());
        }
    }
}
