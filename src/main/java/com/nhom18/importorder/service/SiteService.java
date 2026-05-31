package com.nhom18.importorder.service;

import com.nhom18.importorder.dao.IOrderDAO;
import com.nhom18.importorder.dao.ISiteDAO;
import com.nhom18.importorder.dao.IUserDAO;
import com.nhom18.importorder.dao.impl.SQLiteOrderDAO;
import com.nhom18.importorder.dao.impl.SQLiteSiteDAO;
import com.nhom18.importorder.dao.impl.SQLiteUserDAO;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.model.entity.User;
import com.nhom18.importorder.model.enums.OrderStatus;
import com.nhom18.importorder.model.enums.UserRole;
import java.util.List;

public class SiteService {
    private final ISiteDAO siteDAO;
    private final IOrderDAO orderDAO;
    private final IUserDAO userDAO;

    public SiteService() {
        this.siteDAO = new SQLiteSiteDAO();
        this.orderDAO = new SQLiteOrderDAO();
        this.userDAO = new SQLiteUserDAO();
    }

    public SiteService(ISiteDAO siteDAO, IOrderDAO orderDAO) {
        this(siteDAO, orderDAO, new SQLiteUserDAO());
    }

    public SiteService(ISiteDAO siteDAO, IOrderDAO orderDAO, IUserDAO userDAO) {
        this.siteDAO = siteDAO;
        this.orderDAO = orderDAO;
        this.userDAO = userDAO;
    }

    public List<Site> getAllSites() { return siteDAO.getAll(); }
    public List<Site> getAllActiveSites() { return siteDAO.getAllActive(); }

    public Site getSiteByCode(String siteCode) {
        if (siteCode == null || siteCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã Site không hợp lệ!");
        }
        return siteDAO.getByCode(siteCode);
    }

    public void createSite(Site site) {
        validateSite(site);
        Site existing = siteDAO.getByCode(site.getSiteCode());
        if (existing != null) {
            throw new IllegalArgumentException("Mã đối tác Site '" + site.getSiteCode() + "' đã tồn tại trên hệ thống!");
        }
        siteDAO.insert(site);

        User newUser = new User();
        newUser.setUsername("site_" + site.getSiteCode().toLowerCase());
        newUser.setPasswordHash("8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92"); // "123456"
        newUser.setFullName("Đại diện Site " + site.getName());
        newUser.setRole(UserRole.SITE);
        newUser.setSiteCode(site.getSiteCode());
        newUser.setActive(site.isActive());

        User existingUser = userDAO.getByUsername(newUser.getUsername());
        if (existingUser == null) {
            userDAO.insert(newUser);
        }
    }

    public void updateSite(Site site) {
        validateSite(site);
        Site existing = siteDAO.getByCode(site.getSiteCode());
        if (existing == null) {
            throw new IllegalArgumentException("Đối tác Site '" + site.getSiteCode() + "' không tồn tại!");
        }
        siteDAO.update(site);

        User user = userDAO.getByUsername("site_" + site.getSiteCode().toLowerCase());
        if (user != null) {
            user.setFullName("Đại diện Site " + site.getName());
            userDAO.update(user);
        }
    }

    public void toggleSiteActiveStatus(String siteCode) {
        Site site = siteDAO.getByCode(siteCode);
        if (site == null) throw new IllegalArgumentException("Đối tác Site không tồn tại!");

        boolean currentActive = site.isActive();
        if (currentActive) {
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

        site.setActive(!currentActive);
        siteDAO.update(site);

        User user = userDAO.getByUsername("site_" + siteCode.toLowerCase());
        if (user != null) {
            user.setActive(site.isActive());
            userDAO.update(user);
        }
    }

    private void validateSite(Site site) {
        if (site == null) throw new IllegalArgumentException("Thông tin đối tác Site không được trống!");
        if (site.getSiteCode() == null || site.getSiteCode().trim().isEmpty()) throw new IllegalArgumentException("Mã đối tác Site không được để trống!");
        if (site.getName() == null || site.getName().trim().isEmpty()) throw new IllegalArgumentException("Tên đối tác Site không được để trống!");
        if (site.getShipDays() < 0) throw new IllegalArgumentException("Số ngày vận chuyển đường biển không thể nhỏ hơn 0!");
        if (site.getAirDays() < 0) throw new IllegalArgumentException("Số ngày vận chuyển đường hàng không không thể nhỏ hơn 0!");
    }
}
