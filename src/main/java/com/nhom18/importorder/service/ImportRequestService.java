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
            
            // Thêm các dòng chi tiết mặt hàng và thực hiện kiểm tra MRP (trừ tồn kho nội bộ)
            for (ImportRequestItem item : request.getItems()) {
                item.setRequestId(requestId);
                
                // --- MRP LOGIC: Trừ kho nội bộ ---
                com.nhom18.importorder.model.entity.CompanyInventory stock = companyInventoryDAO.getByMerchandiseCode(item.getMerchandiseCode());
                int orderedQty = item.getQuantityOrdered();
                int shortage = orderedQty;
                
                if (stock != null && stock.getInStockQuantity() > 0) {
                    int available = stock.getInStockQuantity();
                    int fulfilled = Math.min(orderedQty, available);
                    shortage = orderedQty - fulfilled;
                    
                    // Cập nhật tồn kho nội bộ sau khi trừ
                    companyInventoryDAO.updateStock(item.getMerchandiseCode(), available - fulfilled);
                    System.out.println("MRP: Đã tự động đáp ứng " + fulfilled + " " + item.getUnit() + 
                                       " của mặt hàng " + item.getMerchandiseCode() + " từ kho nội bộ. Lượng tồn kho mới: " + (available - fulfilled));
                }
                
                // Thiết lập lượng shortage còn thiếu cần đặt hàng quốc tế
                item.setQuantityShortage(shortage);
                
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

    /**
     * Từ chối phiếu yêu cầu nhập hàng, đồng thời hoàn trả lại tồn kho nội bộ đã khấu trừ tạm tính (MRP rollback)
     */
    public void rejectImportRequest(int requestId) throws Exception {
        ImportRequest req = requestDAO.getById(requestId);
        if (req == null) {
            throw new IllegalArgumentException("Không tìm thấy phiếu yêu cầu với mã: " + requestId);
        }
        if (req.getStatus() == RequestStatus.REJECTED) {
            return; // Đã từ chối rồi
        }

        Connection conn = DatabaseConnection.getInstance().getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            conn.setAutoCommit(false);
            
            // 1. Cập nhật trạng thái phiếu yêu cầu sang REJECTED
            requestDAO.updateStatus(requestId, RequestStatus.REJECTED);
            
            // 2. Hoàn trả lại số lượng đã đáp ứng từ kho nội bộ
            for (ImportRequestItem item : req.getItems()) {
                int fulfilledQty = item.getQuantityOrdered() - item.getQuantityShortage();
                if (fulfilledQty > 0) {
                    com.nhom18.importorder.model.entity.CompanyInventory stock = companyInventoryDAO.getByMerchandiseCode(item.getMerchandiseCode());
                    if (stock != null) {
                        companyInventoryDAO.updateStock(item.getMerchandiseCode(), stock.getInStockQuantity() + fulfilledQty);
                        System.out.println("MRP Rollback: Đã hoàn trả lại " + fulfilledQty + " " + item.getUnit() + 
                                           " của mặt hàng " + item.getMerchandiseCode() + " vào kho nội bộ.");
                    }
                }
            }
            
            conn.commit();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            throw e;
        } finally {
            try {
                conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException autoCommitEx) {
                autoCommitEx.printStackTrace();
            }
        }
    }
}
