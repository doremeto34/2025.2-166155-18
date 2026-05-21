package com.nhom18.importorder.controller.bpbh;

import com.nhom18.importorder.model.entity.Merchandise;
import com.nhom18.importorder.service.MerchandiseService;
import com.nhom18.importorder.util.AlertHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class MerchandiseDialogController {

    @FXML
    private Label lblTitle;
    @FXML
    private TextField txtCode;
    @FXML
    private TextField txtName;
    @FXML
    private TextField txtUnit;
    @FXML
    private TextField txtPrice;
    @FXML
    private TextArea txtDescription;

    private final MerchandiseService merchandiseService;
    private Stage stage;
    private Merchandise existingMerchandise;
    private boolean saved = false;

    public MerchandiseDialogController() {
        this.merchandiseService = new MerchandiseService();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setMerchandise(Merchandise merchandise) {
        this.existingMerchandise = merchandise;
        if (merchandise != null) {
            lblTitle.setText("📝 CẬP NHẬT MẶT HÀNG");
            txtCode.setText(merchandise.getMerchandiseCode());
            txtCode.setDisable(true); // Khóa mã mặt hàng (khóa chính)
            txtName.setText(merchandise.getName());
            txtUnit.setText(merchandise.getUnit());
            txtPrice.setText(String.valueOf(merchandise.getPrice()));
            txtDescription.setText(merchandise.getDescription());
        } else {
            lblTitle.setText("➕ THÊM MỚI MẶT HÀNG");
            txtCode.setDisable(false);
            txtCode.clear();
            txtName.clear();
            txtUnit.clear();
            txtPrice.clear();
            txtDescription.clear();
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        String code = txtCode.getText() != null ? txtCode.getText().trim() : "";
        String name = txtName.getText() != null ? txtName.getText().trim() : "";
        String unit = txtUnit.getText() != null ? txtUnit.getText().trim() : "";
        String priceStr = txtPrice.getText() != null ? txtPrice.getText().trim() : "";
        String description = txtDescription.getText() != null ? txtDescription.getText().trim() : "";

        // 1. Kiểm tra validate phía client
        if (existingMerchandise == null && code.isEmpty()) {
            AlertHelper.showError("Lỗi nhập liệu", "Mã mặt hàng không được để trống!");
            txtCode.requestFocus();
            return;
        }

        if (existingMerchandise == null && code.contains(" ")) {
            AlertHelper.showError("Lỗi nhập liệu", "Mã mặt hàng không được phép chứa khoảng trắng!");
            txtCode.requestFocus();
            return;
        }

        if (name.isEmpty()) {
            AlertHelper.showError("Lỗi nhập liệu", "Tên mặt hàng không được để trống!");
            txtName.requestFocus();
            return;
        }

        if (unit.isEmpty()) {
            AlertHelper.showError("Lỗi nhập liệu", "Đơn vị tính không được để trống!");
            txtUnit.requestFocus();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            AlertHelper.showError("Lỗi nhập liệu", "Đơn giá sản phẩm phải là một số lớn hơn hoặc bằng 0!");
            txtPrice.requestFocus();
            return;
        }

        try {
            if (existingMerchandise == null) {
                // Thêm mới mặt hàng
                Merchandise newMerchandise = new Merchandise(code, name, description, unit, price, true);
                merchandiseService.createMerchandise(newMerchandise);
                AlertHelper.showInfo("Thành công", "Đã thêm mới mặt hàng '" + name + "' thành công!");
            } else {
                // Cập nhật mặt hàng
                existingMerchandise.setName(name);
                existingMerchandise.setUnit(unit);
                existingMerchandise.setPrice(price);
                existingMerchandise.setDescription(description);

                merchandiseService.updateMerchandise(existingMerchandise);
                AlertHelper.showInfo("Thành công", "Đã cập nhật thông tin mặt hàng '" + name + "' thành công!");
            }

            saved = true;
            if (stage != null) {
                stage.close();
            }
        } catch (IllegalArgumentException e) {
            AlertHelper.showError("Lỗi nghiệp vụ", e.getMessage());
        } catch (Exception e) {
            AlertHelper.showError("Lỗi hệ thống", "Không thể lưu thông tin mặt hàng:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if (stage != null) {
            stage.close();
        }
    }
}
