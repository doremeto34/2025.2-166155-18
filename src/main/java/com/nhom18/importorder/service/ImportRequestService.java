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
    private final com.nhom18.importorder.dao.ICompanyInventoryDAO companyInventoryDAO;

    public ImportRequestService() {
        this.requestDAO = new SQLiteImportRequestDAO();
        this.companyInventoryDAO = new com.nhom18.importorder.dao.impl.SQLiteCompanyInventoryDAO();
    }

    public List<ImportRequest> getAllRequests() {
        return requestDAO.getAllWithCreatorName();
    }

    public ImportRequest getRequestById(int id) {
        return requestDAO.getById(id);
    }

    public void createImportRequest(ImportRequest request) throws Exception {
        if (request == null) throw new IllegalArgumentException("Yêu cầu nhập hàng không được để trống.");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new IllegalArgumentException("Yêu cầu phải chứa ít nhất một mặt hàng.");
        
        LocalDate today = LocalDate.now();
        for (ImportRequestItem item : request.getItems()) {
            if (item.getMerchandiseCode() == null || item.getMerchandiseCode().trim().isEmpty()) throw new IllegalArgumentException("Mã mặt hàng không hợp lệ.");
            if (item.getQuantityOrdered() <= 0) throw new IllegalArgumentException("Số lượng đặt của mặt hàng " + item.getMerchandiseCode() + " phải lớn hơn 0.");
            if (item.getDesiredDeliveryDate() == null) throw new IllegalArgumentException("Ngày giao mong muốn không được trống.");
            if (!item.getDesiredDeliveryDate().isAfter(today)) throw new IllegalArgumentException("Ngày nhận mong muốn cho mặt hàng " + item.getMerchandiseCode() + " (" + item.getDesiredDeliveryDate() + ") phải là ngày trong tương lai (sau " + today + ").");
        }

        request.setStatus(RequestStatus.PENDING);
        request.setCreatedDate(today);

        Connection conn = DatabaseConnection.getInstance().getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            int requestId = requestDAO.insert(request);
            if (requestId == -1) throw new SQLException("Không thể lưu thông tin phiếu yêu cầu nhập hàng.");
            request.setId(requestId);
            
            for (ImportRequestItem item : request.getItems()) {
                item.setRequestId(requestId);
                int shortage = ShortageCalculator.deductInventoryAndGetShortage(companyInventoryDAO, item);
                item.setQuantityShortage(shortage);
                requestDAO.insertItem(item);
            }
            conn.commit();
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException rollbackEx) { rollbackEx.printStackTrace(); }
            throw e;
        } finally {
            try { conn.setAutoCommit(originalAutoCommit); } catch (SQLException autoCommitEx) { autoCommitEx.printStackTrace(); }
        }
    }

    public void rejectImportRequest(int requestId) throws Exception {
        ImportRequest req = requestDAO.getById(requestId);
        if (req == null) throw new IllegalArgumentException("Không tìm thấy phiếu yêu cầu với mã: " + requestId);
        if (req.getStatus() == RequestStatus.REJECTED) return;

        Connection conn = DatabaseConnection.getInstance().getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            requestDAO.updateStatus(requestId, RequestStatus.REJECTED);
            for (ImportRequestItem item : req.getItems()) {
                ShortageCalculator.rollbackInventory(companyInventoryDAO, item);
            }
            conn.commit();
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException rollbackEx) { rollbackEx.printStackTrace(); }
            throw e;
        } finally {
            try { conn.setAutoCommit(originalAutoCommit); } catch (SQLException autoCommitEx) { autoCommitEx.printStackTrace(); }
        }
    }
}
