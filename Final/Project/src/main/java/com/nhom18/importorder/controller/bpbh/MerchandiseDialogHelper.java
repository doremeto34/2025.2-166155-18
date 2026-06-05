package com.nhom18.importorder.controller.bpbh;

import com.nhom18.importorder.model.entity.Merchandise;
import com.nhom18.importorder.util.AlertHelper;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MerchandiseDialogHelper {

    public static void openMerchandiseDialog(TableView<Merchandise> tblMerchandise, Merchandise m, Runnable onLoad) {
        try {
            FXMLLoader loader = new FXMLLoader(MerchandiseDialogHelper.class.getResource("/com/nhom18/importorder/view/bpbh/merchandise_dialog.fxml"));
            Parent root = loader.load();
            MerchandiseDialogController controller = loader.getController();
            controller.setMerchandise(m);

            Stage stage = new Stage();
            stage.setTitle(m == null ? "Thêm Mặt Hàng Mới" : "Cập Nhật Thông Tin Mặt Hàng");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(tblMerchandise.getScene().getWindow());

            Scene scene = new Scene(root);
            try { scene.getStylesheets().add(MerchandiseDialogHelper.class.getResource("/css/styles.css").toExternalForm()); } catch (Exception ignored) {}
            stage.setScene(scene);
            controller.setStage(stage);
            stage.showAndWait();

            if (controller.isSaved()) {
                onLoad.run();
            }
        } catch (Exception e) {
            AlertHelper.showError("Lỗi hệ thống", "Không thể mở hộp thoại nhập liệu:\n" + e.getMessage());
        }
    }
}
