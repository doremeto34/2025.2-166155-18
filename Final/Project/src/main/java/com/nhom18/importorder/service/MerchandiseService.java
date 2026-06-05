package com.nhom18.importorder.service;

import com.nhom18.importorder.dao.IMerchandiseDAO;
import com.nhom18.importorder.dao.impl.SQLiteMerchandiseDAO;
import com.nhom18.importorder.model.entity.Merchandise;
import java.util.List;
import java.util.stream.Collectors;

public class MerchandiseService {
    private final IMerchandiseDAO merchandiseDAO;

    public MerchandiseService() {
        this.merchandiseDAO = new SQLiteMerchandiseDAO();
    }

    public MerchandiseService(IMerchandiseDAO merchandiseDAO) {
        this.merchandiseDAO = merchandiseDAO;
    }

    public List<Merchandise> getAllActiveMerchandise() {
        return merchandiseDAO.getAllActive();
    }

    public List<Merchandise> getAllMerchandise() {
        return merchandiseDAO.getAll();
    }

    public Merchandise getMerchandiseByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã mặt hàng không hợp lệ!");
        }
        return merchandiseDAO.getByCode(code.trim());
    }

    public List<Merchandise> searchActiveMerchandise(String query) {
        List<Merchandise> all = merchandiseDAO.getAllActive();
        if (query == null || query.trim().isEmpty()) {
            return all;
        }
        String lowerQuery = query.toLowerCase().trim();
        return all.stream()
            .filter(m -> m.getMerchandiseCode().toLowerCase().contains(lowerQuery) 
                      || m.getName().toLowerCase().contains(lowerQuery)
                      || (m.getDescription() != null && m.getDescription().toLowerCase().contains(lowerQuery)))
            .collect(Collectors.toList());
    }

    public List<Merchandise> searchAllMerchandise(String query, String statusFilter) {
        List<Merchandise> all = merchandiseDAO.getAll();
        String keyword = query != null ? query.toLowerCase().trim() : "";
        String status = statusFilter != null ? statusFilter : "Tất cả";

        return all.stream()
            .filter(m -> {
                boolean matchesKeyword = keyword.isEmpty() ||
                    m.getMerchandiseCode().toLowerCase().contains(keyword) ||
                    m.getName().toLowerCase().contains(keyword) ||
                    (m.getDescription() != null && m.getDescription().toLowerCase().contains(keyword));

                boolean matchesStatus = true;
                if ("Đang kinh doanh".equals(status)) {
                    matchesStatus = m.isActive();
                } else if ("Ngừng kinh doanh".equals(status)) {
                    matchesStatus = !m.isActive();
                }
                return matchesKeyword && matchesStatus;
            })
            .collect(Collectors.toList());
    }

    public void createMerchandise(Merchandise merchandise) {
        validateMerchandise(merchandise);
        Merchandise existing = merchandiseDAO.getByCode(merchandise.getMerchandiseCode().trim());
        if (existing != null) {
            throw new IllegalArgumentException("Mã sản phẩm '" + merchandise.getMerchandiseCode() + "' đã tồn tại trên hệ thống!");
        }
        merchandise.setMerchandiseCode(merchandise.getMerchandiseCode().trim());
        merchandise.setActive(true);
        merchandiseDAO.insert(merchandise);
    }

    public void updateMerchandise(Merchandise merchandise) {
        validateMerchandise(merchandise);
        Merchandise existing = merchandiseDAO.getByCode(merchandise.getMerchandiseCode().trim());
        if (existing == null) {
            throw new IllegalArgumentException("Mặt hàng không tồn tại trên hệ thống!");
        }
        merchandise.setActive(existing.isActive());
        merchandiseDAO.update(merchandise);
    }

    public void toggleMerchandiseActiveStatus(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã mặt hàng không hợp lệ!");
        }
        Merchandise existing = merchandiseDAO.getByCode(code.trim());
        if (existing == null) {
            throw new IllegalArgumentException("Mặt hàng không tồn tại trên hệ thống!");
        }
        if (existing.isActive()) {
            boolean isUsed = merchandiseDAO.isUsedInPendingRequests(existing.getMerchandiseCode());
            if (isUsed) {
                throw new IllegalStateException("Hệ thống từ chối ngừng kinh doanh mặt hàng '" + existing.getName() + 
                    "' vì đang nằm trong các yêu cầu nhập hàng chưa hoàn tất (PENDING hoặc PROCESSING)!");
            }
        }
        existing.setActive(!existing.isActive());
        merchandiseDAO.update(existing);
    }

    private void validateMerchandise(Merchandise m) {
        if (m == null) throw new IllegalArgumentException("Thông tin mặt hàng không được trống!");
        if (m.getMerchandiseCode() == null || m.getMerchandiseCode().trim().isEmpty()) throw new IllegalArgumentException("Mã mặt hàng không được để trống!");
        if (m.getMerchandiseCode().contains(" ")) throw new IllegalArgumentException("Mã mặt hàng không được phép chứa khoảng trắng!");
        if (m.getName() == null || m.getName().trim().isEmpty()) throw new IllegalArgumentException("Tên mặt hàng không được để trống!");
        if (m.getUnit() == null || m.getUnit().trim().isEmpty()) throw new IllegalArgumentException("Đơn vị tính không được để trống!");
        if (m.getPrice() < 0) throw new IllegalArgumentException("Đơn giá sản phẩm không được nhỏ hơn 0!");
    }
}
