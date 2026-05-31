package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.util.AlertHelper;
import java.time.LocalDate;
import java.util.ArrayList;

public class FreeRequestSaver {

    public static void save(CreateFreeRequestController controller) {
        if (!controller.isCheckPassed()) {
            AlertHelper.showWarning("Chưa kiểm tra", "Vui lòng bấm 'Kiểm Tra' để xác nhận tồn kho trước khi lưu đơn!");
            return;
        }

        if (controller.getProposedOrdersData().isEmpty()) {
            AlertHelper.showWarning("Không có đơn hàng", "Vui lòng chạy phân bổ tự động hoặc bấm Add Order để thiết lập đơn hàng trước!");
            return;
        }

        LocalDate reqDate = controller.getDpRequiredDate().getValue();
        if (reqDate == null) {
            AlertHelper.showWarning("Chưa chọn ngày", "Vui lòng chọn ngày giao hàng mong muốn!");
            return;
        }

        boolean confirm = AlertHelper.showConfirm("Xác nhận lưu tất cả", 
            "Hệ thống sẽ tiến hành lưu phiếu yêu cầu tự do và tạo đồng thời " + controller.getProposedOrdersData().size() + " đơn hàng gửi cho các Site.\nBạn có chắc chắn?");
        
        if (confirm) {
            try {
                controller.getOrderService().saveCustomFreeRequestAndOrders(reqDate, new ArrayList<>(controller.getProposedOrdersData()));
                
                AlertHelper.showInfo("Thành công", "Đã lưu thành công phiếu yêu cầu đặt hàng tự do và tạo " + controller.getProposedOrdersData().size() + " đơn đặt hàng gửi Site!");

                controller.getSelectedItemsData().clear();
                controller.getProposedOrdersData().clear();
                controller.getVboxOrdersContainer().getChildren().clear();
                controller.updateTotalQuantityLabel();
                controller.getLblOrdersCount().setText("Danh Sách Đơn Hàng Đề Xuất (0)");
                controller.getDpRequiredDate().setValue(LocalDate.now().plusDays(10));
                
                controller.setCheckPassed(false);
                controller.getBtnSaveAll().setDisable(true);

            } catch (Exception e) {
                AlertHelper.showError("Lỗi lưu dữ liệu", "Không thể lưu đơn hàng: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
