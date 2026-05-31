package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.util.AlertHelper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FreeRequestAllocator {

    public static void allocate(CreateFreeRequestController controller) {
        if (controller.getSelectedItemsData().isEmpty()) {
            AlertHelper.showWarning("Danh sách trống", "Vui lòng chọn ít nhất một mặt hàng bên cột trái để phân bổ!");
            return;
        }

        LocalDate reqDate = controller.getDpRequiredDate().getValue();
        if (reqDate == null) {
            AlertHelper.showWarning("Chưa chọn ngày", "Vui lòng chọn ngày giao hàng mong muốn!");
            return;
        }
        if (!reqDate.isAfter(LocalDate.now())) {
            AlertHelper.showWarning("Ngày giao không hợp lệ", "Ngày giao mong muốn phải ở tương lai!");
            return;
        }

        for (ImportRequestItem item : controller.getSelectedItemsData()) {
            item.setDesiredDeliveryDate(reqDate);
        }

        try {
            List<Order> proposed = controller.getOrderService().generateProposedOrders(new ArrayList<>(controller.getSelectedItemsData()));
            controller.getProposedOrdersData().setAll(proposed);

            controller.getBoxError().setVisible(false);
            controller.getBoxError().setManaged(false);

            controller.renderOrderCards();
            controller.setCheckPassed(true);
            controller.getBtnSaveAll().setDisable(false);

        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            controller.getLblAllocationError().setText(e.getMessage());
            controller.getBoxError().setVisible(true);
            controller.getBoxError().setManaged(true);

            controller.getProposedOrdersData().clear();
            controller.getVboxOrdersContainer().getChildren().clear();
            controller.getLblOrdersCount().setText("Danh Sách Đơn Hàng Đề Xuất (0)");
            
            controller.setCheckPassed(false);
            controller.getBtnSaveAll().setDisable(true);
        }
    }
}
