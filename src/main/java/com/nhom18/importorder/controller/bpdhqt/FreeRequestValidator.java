package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.entity.SiteInventory;
import com.nhom18.importorder.util.AlertHelper;

public class FreeRequestValidator {

    public static void validate(CreateFreeRequestController controller) {
        if (controller.getProposedOrdersData().isEmpty()) {
            AlertHelper.showWarning("Không có đơn hàng", "Vui lòng chạy phân bổ tự động hoặc thêm đơn hàng trước khi kiểm tra!");
            return;
        }

        for (Order order : controller.getProposedOrdersData()) {
            String siteCode = order.getSiteCode();
            String siteName = order.getSiteName();

            if (order.getItems().isEmpty()) {
                AlertHelper.showWarning("Đơn hàng rỗng", "Đơn hàng gửi cho Site " + siteName + " chưa có mặt hàng nào!");
                controller.setCheckPassed(false);
                controller.getBtnSaveAll().setDisable(true);
                return;
            }

            for (OrderItem item : order.getItems()) {
                SiteInventory inventory = controller.getSiteInventoryDAO().get(siteCode, item.getMerchandiseCode());
                int inStock = (inventory != null) ? inventory.getInStockQuantity() : 0;

                if (inStock < item.getQuantityOrdered()) {
                    AlertHelper.showError("Không đủ tồn kho", 
                        String.format("Site '%s' (%s) không đủ tồn kho cho mặt hàng '%s' (%s)!\n" +
                        "Yêu cầu: %d, Hiện có: %d", 
                        siteName, siteCode, item.getMerchandiseName(), item.getMerchandiseCode(), 
                        item.getQuantityOrdered(), inStock));
                    controller.setCheckPassed(false);
                    controller.getBtnSaveAll().setDisable(true);
                    return;
                }
            }
        }

        controller.setCheckPassed(true);
        controller.getBtnSaveAll().setDisable(false);
        FreeRequestSyncer.sync(controller);

        AlertHelper.showInfo("Kiểm tra thành công", 
            "Tất cả các đơn hàng đều hợp lệ và đủ tồn kho tại các Site đối tác!\n" +
            "Danh sách mặt hàng yêu cầu bên trái đã được đồng bộ chính xác.\n" +
            "Bạn có thể tiến hành lưu toàn bộ đơn hàng.");
    }
}
