package com.nhom18.importorder.service;

import static org.junit.jupiter.api.Assertions.*;

import com.nhom18.importorder.dao.IOrderDAO;
import com.nhom18.importorder.dao.ISiteDAO;
import com.nhom18.importorder.dao.IUserDAO;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.model.entity.User;
import com.nhom18.importorder.model.enums.OrderStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SiteServiceTest {

    private SiteService siteService;
    private MockSiteDAO mockSiteDAO;
    private MockOrderDAO mockOrderDAO;
    private MockUserDAO mockUserDAO;

    @BeforeEach
    public void setUp() {
        mockSiteDAO = new MockSiteDAO();
        mockOrderDAO = new MockOrderDAO();
        mockUserDAO = new MockUserDAO();
        siteService = new SiteService(mockSiteDAO, mockOrderDAO, mockUserDAO);
    }

    @Test
    public void testGetAllSites() {
        Site s1 = new Site("TOKYO", "Tokyo Site", 10, 2, "Japan", true);
        Site s2 = new Site("SEOUL", "Seoul Site", 8, 2, "Korea", false);
        mockSiteDAO.sites.add(s1);
        mockSiteDAO.sites.add(s2);

        List<Site> result = siteService.getAllSites();
        assertEquals(2, result.size());
    }

    @Test
    public void testGetAllActiveSites() {
        Site s1 = new Site("TOKYO", "Tokyo Site", 10, 2, "Japan", true);
        Site s2 = new Site("SEOUL", "Seoul Site", 8, 2, "Korea", false);
        mockSiteDAO.sites.add(s1);
        mockSiteDAO.sites.add(s2);

        List<Site> result = siteService.getAllActiveSites();
        assertEquals(1, result.size());
        assertEquals("TOKYO", result.get(0).getSiteCode());
    }

    @Test
    public void testGetSiteByCode() {
        Site s1 = new Site("TOKYO", "Tokyo Site", 10, 2, "Japan", true);
        mockSiteDAO.sites.add(s1);

        Site result = siteService.getSiteByCode("TOKYO");
        assertNotNull(result);
        assertEquals("Tokyo Site", result.getName());

        assertThrows(IllegalArgumentException.class, () -> siteService.getSiteByCode(""));
        assertThrows(IllegalArgumentException.class, () -> siteService.getSiteByCode(null));
    }

    @Test
    public void testCreateSiteSuccessfully() {
        Site newSite = new Site("BANGKOK", "Bangkok Site", 5, 2, "Thailand", true);
        siteService.createSite(newSite);

        assertEquals(1, mockSiteDAO.sites.size());
        assertEquals("BANGKOK", mockSiteDAO.sites.get(0).getSiteCode());
    }

    @Test
    public void testCreateSiteDuplicateCodeThrowsException() {
        Site s1 = new Site("TOKYO", "Tokyo Site", 10, 2, "Japan", true);
        mockSiteDAO.sites.add(s1);

        Site duplicate = new Site("TOKYO", "Another Tokyo", 5, 1, "Duplicate", true);
        assertThrows(IllegalArgumentException.class, () -> siteService.createSite(duplicate));
    }

    @Test
    public void testCreateSiteInvalidDataThrowsException() {
        // Tên trống
        Site s1 = new Site("TOKYO", "", 10, 2, "Japan", true);
        assertThrows(IllegalArgumentException.class, () -> siteService.createSite(s1));

        // Số ngày ship âm
        Site s2 = new Site("TOKYO", "Tokyo Site", -1, 2, "Japan", true);
        assertThrows(IllegalArgumentException.class, () -> siteService.createSite(s2));

        // Số ngày air âm
        Site s3 = new Site("TOKYO", "Tokyo Site", 10, -5, "Japan", true);
        assertThrows(IllegalArgumentException.class, () -> siteService.createSite(s3));
    }

    @Test
    public void testUpdateSiteSuccessfully() {
        Site s1 = new Site("TOKYO", "Tokyo Site", 10, 2, "Japan", true);
        mockSiteDAO.sites.add(s1);

        Site updated = new Site("TOKYO", "Tokyo Main Site", 12, 3, "Japan - Updated", true);
        siteService.updateSite(updated);

        Site result = mockSiteDAO.getByCode("TOKYO");
        assertEquals("Tokyo Main Site", result.getName());
        assertEquals(12, result.getShipDays());
        assertEquals(3, result.getAirDays());
        assertEquals("Japan - Updated", result.getOtherInfo());
    }

    @Test
    public void testUpdateSiteNonExistentThrowsException() {
        Site nonExistent = new Site("HANOI", "Hanoi Site", 1, 1, "VN", true);
        assertThrows(IllegalArgumentException.class, () -> siteService.updateSite(nonExistent));
    }

    @Test
    public void testToggleActiveStatusToInactiveSuccessfullyWhenNoActiveOrders() {
        // Site đang hoạt động
        Site s1 = new Site("TOKYO", "Tokyo Site", 10, 2, "Japan", true);
        mockSiteDAO.sites.add(s1);

        // Có đơn hàng nhưng ở trạng thái DELIVERED hoặc CANCELLED (Không phải dở dang)
        Order o1 = new Order();
        o1.setId(1);
        o1.setSiteCode("TOKYO");
        o1.setStatus(OrderStatus.DELIVERED);

        Order o2 = new Order();
        o2.setId(2);
        o2.setSiteCode("TOKYO");
        o2.setStatus(OrderStatus.CANCELLED);

        mockOrderDAO.orders.add(o1);
        mockOrderDAO.orders.add(o2);

        // Thực hiện tắt hoạt động
        siteService.toggleSiteActiveStatus("TOKYO");

        assertFalse(s1.isActive(), "Site phải chuyển sang Inactive");
    }

    @Test
    public void testToggleActiveStatusToInactiveFailedWhenHasActiveOrders() {
        // Site đang hoạt động
        Site s1 = new Site("TOKYO", "Tokyo Site", 10, 2, "Japan", true);
        mockSiteDAO.sites.add(s1);

        // Có đơn hàng dở dang ở trạng thái PENDING
        Order o1 = new Order();
        o1.setId(1);
        o1.setSiteCode("TOKYO");
        o1.setStatus(OrderStatus.PENDING);

        mockOrderDAO.orders.add(o1);

        // Đổi trạng thái từ Active sang Inactive phải ném ngoại lệ do dở dang đơn hàng
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            siteService.toggleSiteActiveStatus("TOKYO");
        });
        
        assertTrue(exception.getMessage().contains("đang có 1 đơn hàng trong quá trình xử lý"), 
            "Thông báo lỗi phải hiển thị chính xác số lượng đơn hàng dở dang.");
        assertTrue(s1.isActive(), "Site vẫn phải giữ trạng thái Active");
    }

    @Test
    public void testToggleInactiveStatusToActiveSuccessfully() {
        // Site đang không hoạt động
        Site s1 = new Site("TOKYO", "Tokyo Site", 10, 2, "Japan", false);
        mockSiteDAO.sites.add(s1);

        // Kích hoạt lại
        siteService.toggleSiteActiveStatus("TOKYO");

        assertTrue(s1.isActive(), "Site phải chuyển sang Active");
    }

    @Test
    public void testCreateSiteAutomaticallyCreatesUser() {
        Site newSite = new Site("BANGKOK", "Bangkok Site", 5, 2, "Thailand", true);
        siteService.createSite(newSite);

        // Kiểm tra xem User tương ứng có được tạo không
        User createdUser = mockUserDAO.getByUsername("site_bangkok");
        assertNotNull(createdUser, "Tài khoản đăng nhập tự động cho Site phải tồn tại");
        assertEquals("Đại diện Site Bangkok Site", createdUser.getFullName());
        assertEquals("BANGKOK", createdUser.getSiteCode());
        assertTrue(createdUser.isActive());
    }

    @Test
    public void testUpdateSiteAutomaticallyUpdatesUserFullName() {
        Site s1 = new Site("TOKYO", "Tokyo Site", 10, 2, "Japan", true);
        mockSiteDAO.sites.add(s1);

        // Tạo tài khoản sẵn
        User u1 = new User();
        u1.setUsername("site_tokyo");
        u1.setFullName("Đại diện Site Tokyo");
        u1.setSiteCode("TOKYO");
        u1.setActive(true);
        mockUserDAO.users.add(u1);

        // Cập nhật tên Site
        Site updated = new Site("TOKYO", "Tokyo Premium Site", 10, 2, "Japan", true);
        siteService.updateSite(updated);

        // Xác minh tên User được đồng bộ
        User updatedUser = mockUserDAO.getByUsername("site_tokyo");
        assertNotNull(updatedUser);
        assertEquals("Đại diện Site Tokyo Premium Site", updatedUser.getFullName());
    }

    @Test
    public void testToggleSiteActiveStatusTogglesUserActiveStatus() {
        Site s1 = new Site("TOKYO", "Tokyo Site", 10, 2, "Japan", true);
        mockSiteDAO.sites.add(s1);

        // Tạo tài khoản sẵn
        User u1 = new User();
        u1.setUsername("site_tokyo");
        u1.setFullName("Đại diện Site Tokyo");
        u1.setSiteCode("TOKYO");
        u1.setActive(true);
        mockUserDAO.users.add(u1);

        // Toggle hoạt động (sang Inactive)
        siteService.toggleSiteActiveStatus("TOKYO");

        assertFalse(s1.isActive());
        // Xác minh trạng thái User cũng được đồng bộ sang Inactive
        User updatedUser = mockUserDAO.getByUsername("site_tokyo");
        assertNotNull(updatedUser);
        assertFalse(updatedUser.isActive());
    }

    // --- MOCK DAO IMPLEMENTATIONS FOR TESTING ---
    private static class MockSiteDAO implements ISiteDAO {
        List<Site> sites = new ArrayList<>();

        @Override
        public Site getByCode(String siteCode) {
            if (siteCode == null) return null;
            return sites.stream().filter(s -> siteCode.equalsIgnoreCase(s.getSiteCode())).findFirst().orElse(null);
        }

        @Override
        public List<Site> getAllActive() {
            return sites.stream().filter(Site::isActive).collect(Collectors.toList());
        }

        @Override
        public List<Site> getAll() {
            return sites;
        }

        @Override
        public void insert(Site site) {
            sites.add(site);
        }

        @Override
        public void update(Site site) {
            Site s = getByCode(site.getSiteCode());
            if (s != null) {
                s.setName(site.getName());
                s.setShipDays(site.getShipDays());
                s.setAirDays(site.getAirDays());
                s.setOtherInfo(site.getOtherInfo());
                s.setActive(site.isActive());
            }
        }
    }

    private static class MockOrderDAO implements IOrderDAO {
        List<Order> orders = new ArrayList<>();

        @Override
        public Order getById(int id) {
            return orders.stream().filter(o -> o.getId() == id).findFirst().orElse(null);
        }

        @Override
        public List<Order> getAll() {
            return orders;
        }

        @Override
        public List<Order> getBySiteCode(String siteCode) {
            if (siteCode == null) return new ArrayList<>();
            return orders.stream().filter(o -> siteCode.equalsIgnoreCase(o.getSiteCode())).collect(Collectors.toList());
        }

        @Override
        public List<Order> getByRequestId(int requestId) {
            return orders.stream().filter(o -> o.getRequestId() == requestId).collect(Collectors.toList());
        }

        @Override
        public int insert(Order order) {
            orders.add(order);
            return order.getId();
        }

        @Override
        public void updateStatus(int orderId, OrderStatus status) {
            Order o = getById(orderId);
            if (o != null) {
                o.setStatus(status);
            }
        }

        @Override
        public void updateCancelReason(int orderId, String reason) {
            Order o = getById(orderId);
            if (o != null) {
                o.setCancelReason(reason);
                o.setStatus(OrderStatus.CANCELLED);
            }
        }

        @Override
        public void updateItemQuantities(int orderItemId, int confirmedQty, int receivedQty) {
            // Không cần mock chi tiết cho test này
        }
    }

    private static class MockUserDAO implements IUserDAO {
        List<User> users = new ArrayList<>();

        @Override
        public User getById(int id) {
            return users.stream().filter(u -> u.getId() == id).findFirst().orElse(null);
        }

        @Override
        public User getByUsername(String username) {
            if (username == null) return null;
            return users.stream().filter(u -> username.equalsIgnoreCase(u.getUsername())).findFirst().orElse(null);
        }

        @Override
        public List<User> getAll() {
            return users;
        }

        @Override
        public void insert(User user) {
            users.add(user);
        }

        @Override
        public void update(User user) {
            User u = getByUsername(user.getUsername());
            if (u != null) {
                u.setPasswordHash(user.getPasswordHash());
                u.setFullName(user.getFullName());
                u.setRole(user.getRole());
                u.setSiteCode(user.getSiteCode());
                u.setActive(user.isActive());
            }
        }
    }
}
