package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.util.AlertHelper;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SiteDialogHelper {

    public static void openSiteDialog(TableView<Site> tblSites, Site site, Runnable onLoad) {
        try {
            FXMLLoader loader = new FXMLLoader(SiteDialogHelper.class.getResource("/com/nhom18/importorder/view/bpdhqt/site_dialog.fxml"));
            Parent root = loader.load();
            SiteDialogController controller = loader.getController();
            controller.setSite(site);

            Stage stage = new Stage();
            stage.setTitle(site == null ? "Thêm Đối Tác Site Mới" : "Cập Nhật Thông Tin Đối Tác Site");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(tblSites.getScene().getWindow());

            Scene scene = new Scene(root);
            try { scene.getStylesheets().add(SiteDialogHelper.class.getResource("/css/styles.css").toExternalForm()); } catch (Exception ignored) {}
            stage.setScene(scene);
            controller.setStage(stage);
            stage.showAndWait();

            if (controller.isSaved()) {
                onLoad.run();
            }
        } catch (Exception e) {
            AlertHelper.showError("Lỗi hệ thống", "Không thể mở hộp thoại:\n" + e.getMessage());
        }
    }
}
