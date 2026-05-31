package com.nhom18.importorder.controller.admin;

import com.nhom18.importorder.model.entity.User;
import com.nhom18.importorder.util.AlertHelper;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AdminDialogHelper {

    public static void openUserDialog(TableView<User> tblUsers, User user, Runnable onLoad) {
        try {
            FXMLLoader loader = new FXMLLoader(AdminDialogHelper.class.getResource("/com/nhom18/importorder/view/admin/user_dialog.fxml"));
            Parent root = loader.load();
            UserDialogController controller = loader.getController();
            controller.setUser(user);

            Stage stage = new Stage();
            stage.setTitle(user == null ? "Thêm Mới Tài Khoản" : "Cập Nhật Tài Khoản");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(tblUsers.getScene().getWindow());

            Scene scene = new Scene(root);
            try { scene.getStylesheets().add(AdminDialogHelper.class.getResource("/css/styles.css").toExternalForm()); } catch (Exception ignored) {}
            stage.setScene(scene);
            controller.setStage(stage);
            stage.showAndWait();

            if (controller.isSaved()) {
                onLoad.run();
            }
        } catch (Exception e) {
            AlertHelper.showError("Lỗi", "Không mở được hộp thoại: " + e.getMessage());
        }
    }

    public static void openChangePasswordDialog(TableView<User> tblUsers, User selected) {
        try {
            FXMLLoader loader = new FXMLLoader(AdminDialogHelper.class.getResource("/com/nhom18/importorder/view/admin/change_password_dialog.fxml"));
            Parent root = loader.load();
            ChangePasswordDialogController controller = loader.getController();
            controller.setUser(selected);

            Stage stage = new Stage();
            stage.setTitle("Đặt Lại Mật Khẩu");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(tblUsers.getScene().getWindow());

            Scene scene = new Scene(root);
            try { scene.getStylesheets().add(AdminDialogHelper.class.getResource("/css/styles.css").toExternalForm()); } catch (Exception ignored) {}
            stage.setScene(scene);
            controller.setStage(stage);
            stage.showAndWait();
        } catch (Exception e) {
            AlertHelper.showError("Lỗi", "Không mở được hộp thoại đổi mật khẩu: " + e.getMessage());
        }
    }
}
