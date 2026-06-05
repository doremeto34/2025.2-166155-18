package com.nhom18.importorder.service;

import static org.junit.jupiter.api.Assertions.*;

import com.nhom18.importorder.dao.IMerchandiseDAO;
import com.nhom18.importorder.model.entity.Merchandise;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MerchandiseServiceTest {

    private MerchandiseService merchandiseService;
    private MockMerchandiseDAO mockMerchandiseDAO;

    @BeforeEach
    public void setUp() {
        mockMerchandiseDAO = new MockMerchandiseDAO();
        merchandiseService = new MerchandiseService(mockMerchandiseDAO);
    }

    @Test
    public void testGetAllMerchandise() {
        Merchandise m1 = new Merchandise("M_CPU_I7", "Intel Core i7", "CPU i7", "Cái", 350.0, true);
        Merchandise m2 = new Merchandise("M_CPU_I9", "Intel Core i9", "CPU i9", "Cái", 580.0, false);
        mockMerchandiseDAO.merchandises.add(m1);
        mockMerchandiseDAO.merchandises.add(m2);

        List<Merchandise> result = merchandiseService.getAllMerchandise();
        assertEquals(2, result.size());
    }

    @Test
    public void testGetAllActiveMerchandise() {
        Merchandise m1 = new Merchandise("M_CPU_I7", "Intel Core i7", "CPU i7", "Cái", 350.0, true);
        Merchandise m2 = new Merchandise("M_CPU_I9", "Intel Core i9", "CPU i9", "Cái", 580.0, false);
        mockMerchandiseDAO.merchandises.add(m1);
        mockMerchandiseDAO.merchandises.add(m2);

        List<Merchandise> result = merchandiseService.getAllActiveMerchandise();
        assertEquals(1, result.size());
        assertEquals("M_CPU_I7", result.get(0).getMerchandiseCode());
    }

    @Test
    public void testGetMerchandiseByCode() {
        Merchandise m1 = new Merchandise("M_CPU_I7", "Intel Core i7", "CPU i7", "Cái", 350.0, true);
        mockMerchandiseDAO.merchandises.add(m1);

        Merchandise result = merchandiseService.getMerchandiseByCode("M_CPU_I7");
        assertNotNull(result);
        assertEquals("Intel Core i7", result.getName());

        assertThrows(IllegalArgumentException.class, () -> merchandiseService.getMerchandiseByCode(""));
        assertThrows(IllegalArgumentException.class, () -> merchandiseService.getMerchandiseByCode(null));
    }

    @Test
    public void testCreateMerchandiseSuccessfully() {
        Merchandise newMerchandise = new Merchandise("M_RAM_16G", "Kingston RAM 16GB", "RAM Kingston DDR5", "Thanh", 85.0, true);
        merchandiseService.createMerchandise(newMerchandise);

        assertEquals(1, mockMerchandiseDAO.merchandises.size());
        assertEquals("M_RAM_16G", mockMerchandiseDAO.merchandises.get(0).getMerchandiseCode());
    }

    @Test
    public void testCreateMerchandiseDuplicateCodeThrowsException() {
        Merchandise m1 = new Merchandise("M_CPU_I7", "Intel Core i7", "CPU i7", "Cái", 350.0, true);
        mockMerchandiseDAO.merchandises.add(m1);

        Merchandise duplicate = new Merchandise("M_CPU_I7", "Another Intel Core i7", "Another CPU", "Cái", 360.0, true);
        assertThrows(IllegalArgumentException.class, () -> merchandiseService.createMerchandise(duplicate));
    }

    @Test
    public void testCreateMerchandiseNegativePriceThrowsException() {
        Merchandise invalid = new Merchandise("M_RAM_16G", "Kingston RAM 16GB", "RAM Kingston DDR5", "Thanh", -10.0, true);
        assertThrows(IllegalArgumentException.class, () -> merchandiseService.createMerchandise(invalid));
    }

    @Test
    public void testCreateMerchandiseInvalidDataThrowsException() {
        // Tên trống
        Merchandise m1 = new Merchandise("M_RAM_16G", "", "RAM Kingston DDR5", "Thanh", 85.0, true);
        assertThrows(IllegalArgumentException.class, () -> merchandiseService.createMerchandise(m1));

        // Đơn vị tính trống
        Merchandise m2 = new Merchandise("M_RAM_16G", "Kingston RAM 16GB", "RAM Kingston DDR5", "", 85.0, true);
        assertThrows(IllegalArgumentException.class, () -> merchandiseService.createMerchandise(m2));

        // Mã chứa khoảng trắng
        Merchandise m3 = new Merchandise("M RAM 16G", "Kingston RAM 16GB", "RAM Kingston DDR5", "Thanh", 85.0, true);
        assertThrows(IllegalArgumentException.class, () -> merchandiseService.createMerchandise(m3));
    }

    @Test
    public void testUpdateMerchandiseSuccessfully() {
        Merchandise m1 = new Merchandise("M_CPU_I7", "Intel Core i7", "CPU i7", "Cái", 350.0, true);
        mockMerchandiseDAO.merchandises.add(m1);

        Merchandise updated = new Merchandise("M_CPU_I7", "Intel Core i7 13700K", "CPU i7 Thế hệ 13", "Cái", 370.0, true);
        merchandiseService.updateMerchandise(updated);

        Merchandise result = mockMerchandiseDAO.getByCode("M_CPU_I7");
        assertEquals("Intel Core i7 13700K", result.getName());
        assertEquals("CPU i7 Thế hệ 13", result.getDescription());
        assertEquals(370.0, result.getPrice());
    }

    @Test
    public void testUpdateMerchandiseNonExistentThrowsException() {
        Merchandise nonExistent = new Merchandise("M_SSD_1T", "Samsung 990 Pro 1TB", "SSD NVMe 1TB", "Cái", 120.0, true);
        assertThrows(IllegalArgumentException.class, () -> merchandiseService.updateMerchandise(nonExistent));
    }

    @Test
    public void testToggleActiveStatusSuccessfully() {
        // Mặt hàng đang kinh doanh
        Merchandise m1 = new Merchandise("M_CPU_I7", "Intel Core i7", "CPU i7", "Cái", 350.0, true);
        mockMerchandiseDAO.merchandises.add(m1);
        mockMerchandiseDAO.isUsedInPending = false; // Không nằm trong yêu cầu dở dang

        // Ngừng kinh doanh
        merchandiseService.toggleMerchandiseActiveStatus("M_CPU_I7");
        assertFalse(m1.isActive(), "Mặt hàng phải chuyển sang ngừng hoạt động");

        // Kích hoạt lại
        merchandiseService.toggleMerchandiseActiveStatus("M_CPU_I7");
        assertTrue(m1.isActive(), "Mặt hàng phải chuyển sang đang hoạt động");
    }

    @Test
    public void testToggleActiveStatusFailedWhenUsedInPendingRequests() {
        // Mặt hàng đang kinh doanh
        Merchandise m1 = new Merchandise("M_CPU_I7", "Intel Core i7", "CPU i7", "Cái", 350.0, true);
        mockMerchandiseDAO.merchandises.add(m1);
        mockMerchandiseDAO.isUsedInPending = true; // Ràng buộc: Đang nằm trong yêu cầu dở dang

        // Thực hiện đổi trạng thái sang ngừng kinh doanh phải bị chặn và ném ngoại lệ
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            merchandiseService.toggleMerchandiseActiveStatus("M_CPU_I7");
        });

        assertTrue(exception.getMessage().contains("vì đang nằm trong các yêu cầu nhập hàng chưa hoàn tất"),
            "Thông báo lỗi phải hiển thị chính xác lý do ràng buộc");
        assertTrue(m1.isActive(), "Mặt hàng vẫn phải giữ trạng thái Đang kinh doanh");
    }

    // --- MOCK DAO IMPLEMENTATIONS FOR TESTING ---
    private static class MockMerchandiseDAO implements IMerchandiseDAO {
        List<Merchandise> merchandises = new ArrayList<>();
        boolean isUsedInPending = false;

        @Override
        public List<Merchandise> getAllActive() {
            return merchandises.stream().filter(Merchandise::isActive).collect(Collectors.toList());
        }

        @Override
        public List<Merchandise> getAll() {
            return merchandises;
        }

        @Override
        public Merchandise getByCode(String code) {
            if (code == null) return null;
            return merchandises.stream().filter(m -> code.equalsIgnoreCase(m.getMerchandiseCode())).findFirst().orElse(null);
        }

        @Override
        public void insert(Merchandise merchandise) {
            merchandises.add(merchandise);
        }

        @Override
        public void update(Merchandise merchandise) {
            Merchandise m = getByCode(merchandise.getMerchandiseCode());
            if (m != null) {
                m.setName(merchandise.getName());
                m.setDescription(merchandise.getDescription());
                m.setUnit(merchandise.getUnit());
                m.setPrice(merchandise.getPrice());
                m.setActive(merchandise.isActive());
            }
        }

        @Override
        public boolean isUsedInPendingRequests(String code) {
            return isUsedInPending;
        }
    }
}
