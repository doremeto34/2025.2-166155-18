package com.nhom18.importorder.service;

import com.nhom18.importorder.dao.IOrderDAO;
import com.nhom18.importorder.dao.ISiteDAO;
import com.nhom18.importorder.dao.impl.SQLiteOrderDAO;
import com.nhom18.importorder.dao.impl.SQLiteSiteDAO;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.model.enums.OrderStatus;
import java.util.List;

public class SiteService {
    private final ISiteDAO siteDAO;
    private final IOrderDAO orderDAO;

    public SiteService() {
        this.siteDAO = new SQLiteSiteDAO();
        this.orderDAO = new SQLiteOrderDAO();
    }

    // Constructor phục vụ Mock Testing
    public SiteService(ISiteDAO siteDAO, IOrderDAO orderDAO) {
        this.siteDAO = siteDAO;
        this.orderDAO = orderDAO;
    }

    public List<Site> getAllSites() {
        return siteDAO.getAll();
    }

    public List<Site> getAllActiveSites() {
        return siteDAO.getAllActive();
    }

    public Site getSiteByCode(String siteCode) {
        if (siteCode == null || siteCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã Site không hợp lệ!");
        }
        return siteDAO.getByCode(siteCode);
    }

    public void createSite(Site site) {
        validateSite(site);
        
        // Kiểm tra trùng lặp mã Site
        Site existing = siteDAO.getByCode(site.getSiteCode());
        if (existing != null) {
            throw new IllegalArgumentException("Mã đối tác Site '" + site.getSiteCode() + "' đã tồn tại trên hệ thống!");
        }

        siteDAO.insert(site);
    }

    public void updateSite(Site site) {
        validateSite(site);

        Site existing = siteDAO.getByCode(site.getSiteCode());
        if (existing == null) {
            throw new IllegalArgumentException("Đối tác Site '" + site.getSiteCode() + "' không tồn tại!");
        }

        siteDAO.update(site);
    }

    /**
     * Bật/Tắt trạng thái hoạt động của đối tác Site.
     * Nếu tắt trạng thái (Active -> Inactive), kiểm tra xem có đơn hàng nào dở dang hay không.
     */
    public void toggleSiteActiveStatus(String siteCode) {
        Site site = siteDAO.getByCode(siteCode);
        if (site == null) {
            throw new IllegalArgumentException("Đối tác Site không tồn tại!");
        }

        boolean currentActive = site.isActive();
        if (currentActive) {
            // Chuyển từ Active sang Inactive: Bắt buộc kiểm tra đơn hàng dở dang
            List<Order> orders = orderDAO.getBySiteCode(siteCode);
            long activeOrdersCount = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING || 
                             o.getStatus() == OrderStatus.CONFIRMED || 
                             o.getStatus() == OrderStatus.SHIPPED)
                .count();

            if (activeOrdersCount > 0) {
                throw new IllegalStateException("Không thể ngừng hoạt động đối tác Site '" + site.getName() + 
                    "' vì đang có " + activeOrdersCount + " đơn hàng trong quá trình xử lý (Chờ/Xác nhận/Đang giao)!");
            }
        }

        // Thay đổi trạng thái
        site.setActive(!currentActive);
        siteDAO.update(site);
    }

    private void validateSite(Site site) {
        if (site == null) {
            throw new IllegalArgumentException("Thông tin đối tác Site không được trống!");
        }
        if (site.getSiteCode() == null || site.getSiteCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã đối tác Site không được để trống!");
        }
        if (site.getName() == null || site.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đối tác Site không được để trống!");
        }
        if (site.getShipDays() < 0) {
            throw new IllegalArgumentException("Số ngày vận chuyển đường biển không thể nhỏ hơn 0!");
        }
        if (site.getAirDays() < 0) {
            throw new IllegalArgumentException("Số ngày vận chuyển đường hàng không không thể nhỏ hơn 0!");
        }
    }
}
