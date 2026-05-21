package com.nhom18.importorder.service;

import com.nhom18.importorder.dao.DatabaseConnection;
import com.nhom18.importorder.dao.IImportRequestDAO;
import com.nhom18.importorder.dao.impl.SQLiteImportRequestDAO;
import com.nhom18.importorder.model.entity.ImportRequest;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.enums.RequestStatus;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ImportRequestService {
    private final IImportRequestDAO requestDAO;

    public ImportRequestService() {
        this.requestDAO = new SQLiteImportRequestDAO();
    }

    public List<ImportRequest> getAllRequests() {
        return requestDAO.getAllWithCreatorName();
    }

    public ImportRequest getRequestById(int id) {
        return requestDAO.getById(id);
    }

    /**
     * Tạo yêu cầu nhập hàng với các quy tắc nghiệp vụ nghiêm ngặt (UC3)
     */
    public void createImportRequest(ImportRequest request) throws Exception {
        // 1. Kiểm tra tính hợp lệ của phiếu
        if (request == null) {
            throw new IllegalArgumentException("Yêu cầu nhập hàng không được để trống.");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Yêu cầu phải chứa ít nhất một mặt hàng.");
        }

        LocalDate today = LocalDate.now();
        
        // 2. Kiểm tra từng mặt hàng
        for (ImportRequestItem item : request.getItems()) {
            if (item.getMerchandiseCode() == null || item.getMerchandiseCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Mã mặt hàng không hợp lệ.");
            }
            if (item.getQuantityOrdered() <= 0) {
                throw new IllegalArgumentException("Số lượng đặt của mặt hàng " + item.getMerchandiseCode() + " phải lớn hơn 0.");
            }
            if (item.getDesiredDeliveryDate() == null) {
                throw new IllegalArgumentException("Ngày giao mong muốn không được trống.");
            }
            if (!item.getDesiredDeliveryDate().isAfter(today)) {
                throw new IllegalArgumentException("Ngày nhận mong muốn cho mặt hàng " + item.getMerchandiseCode() + 
                                                   " (" + item.getDesiredDeliveryDate() + ") phải là ngày trong tương lai (sau " + today + ").");
            }
        }

        // Thiết lập trạng thái mặc định ban đầu là PENDING và ngày tạo là hôm nay
        request.setStatus(RequestStatus.PENDING);
        request.setCreatedDate(today);

        // 3. Thực hiện lưu vào CSDL dưới dạng một Transaction nguyên tử
        Connection conn = DatabaseConnection.getInstance().getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            conn.setAutoCommit(false);
            
            // Thêm phiếu yêu cầu chính
            int requestId = requestDAO.insert(request);
            if (requestId == -1) {
                throw new SQLException("Không thể lưu thông tin phiếu yêu cầu nhập hàng.");
            }
            request.setId(requestId);
            
            // Thêm các dòng chi tiết mặt hàng
            for (ImportRequestItem item : request.getItems()) {
                item.setRequestId(requestId);
                requestDAO.insertItem(item);
            }
            
            // Xác nhận lưu thành công
            conn.commit();
            System.out.println("Yêu cầu nhập hàng #" + requestId + " đã được tạo thành công.");
        } catch (Exception e) {
            // Hủy toàn bộ thay đổi nếu có bất kỳ lỗi nào
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            throw e;
        } finally {
            // Trả lại trạng thái auto-commit ban đầu
            try {
                conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException autoCommitEx) {
                autoCommitEx.printStackTrace();
            }
        }
    }
}
