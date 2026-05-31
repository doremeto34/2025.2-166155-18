package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.service.SiteService;
import com.nhom18.importorder.util.AlertHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SiteDialogController {

    @FXML
    private Label lblTitle;
    @FXML
    private TextField txtSiteCode;
    @FXML
    private TextField txtSiteName;
    @FXML
    private TextField txtShipDays;
    @FXML
    private TextField txtAirDays;
    @FXML
    private TextArea txtOtherInfo;

    private final SiteService siteService;
    private Stage stage;
    private Site existingSite;
    private boolean saved = false;

    public SiteDialogController() {
        this.siteService = new SiteService();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setSite(Site site) {
        this.existingSite = site;
        if (site != null) {
            lblTitle.setText("📝 CẬP NHẬT ĐỐI TÁC SITE");
            txtSiteCode.setText(site.getSiteCode());
            txtSiteCode.setDisable(true); // Không cho phép sửa khóa chính
            txtSiteName.setText(site.getName());
            txtShipDays.setText(String.valueOf(site.getShipDays()));
            txtAirDays.setText(String.valueOf(site.getAirDays()));
            txtOtherInfo.setText(site.getOtherInfo());
        } else {
            lblTitle.setText("➕ THÊM MỚI ĐỐI TÁC SITE");
            txtSiteCode.setDisable(false);
            txtSiteCode.clear();
            txtSiteName.clear();
            txtShipDays.clear();
            txtAirDays.clear();
            txtOtherInfo.clear();
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        String siteCode = txtSiteCode.getText() != null ? txtSiteCode.getText().trim() : "";
        String siteName = txtSiteName.getText() != null ? txtSiteName.getText().trim() : "";
        String shipDaysStr = txtShipDays.getText() != null ? txtShipDays.getText().trim() : "";
        String airDaysStr = txtAirDays.getText() != null ? txtAirDays.getText().trim() : "";
        String otherInfo = txtOtherInfo.getText() != null ? txtOtherInfo.getText().trim() : "";

        // 1. Kiểm tra validate đầu vào sơ bộ tại Client
        if (existingSite == null && siteCode.isEmpty()) {
            AlertHelper.showError("Lỗi nhập liệu", "Mã đối tác (Site Code) không được để trống!");
            txtSiteCode.requestFocus();
            return;
        }

        if (siteName.isEmpty()) {
            AlertHelper.showError("Lỗi nhập liệu", "Tên đối tác (Site Name) không được để trống!");
            txtSiteName.requestFocus();
            return;
        }

        int shipDays;
        try {
            shipDays = Integer.parseInt(shipDaysStr);
            if (shipDays < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            AlertHelper.showError("Lỗi nhập liệu", "Số ngày vận chuyển đường biển phải là số nguyên dương hoặc bằng 0!");
            txtShipDays.requestFocus();
            return;
        }

        int airDays;
        try {
            airDays = Integer.parseInt(airDaysStr);
            if (airDays < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            AlertHelper.showError("Lỗi nhập liệu", "Số ngày vận chuyển đường hàng không phải là số nguyên dương hoặc bằng 0!");
            txtAirDays.requestFocus();
            return;
        }

        try {
            if (existingSite == null) {
                // 2. Chế độ thêm mới đối tác
                Site newSite = new Site(siteCode, siteName, shipDays, airDays, otherInfo, true); // Mặc định active = true
                siteService.createSite(newSite);
                AlertHelper.showInfo("Thành công", "Đã thêm mới đối tác Site '" + siteName + "' thành công!");
            } else {
                // 3. Chế độ cập nhật đối tác
                existingSite.setName(siteName);
                existingSite.setShipDays(shipDays);
                existingSite.setAirDays(airDays);
                existingSite.setOtherInfo(otherInfo);
                
                siteService.updateSite(existingSite);
                AlertHelper.showInfo("Thành công", "Đã cập nhật thông tin đối tác Site '" + siteName + "' thành công!");
            }
            
            saved = true;
            if (stage != null) {
                stage.close();
            }
        } catch (IllegalArgumentException e) {
            AlertHelper.showError("Lỗi nghiệp vụ", e.getMessage());
        } catch (Exception e) {
            AlertHelper.showError("Lỗi hệ thống", "Không thể lưu thông tin đối tác:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if (stage != null) {
            stage.close();
        }
    }
}
