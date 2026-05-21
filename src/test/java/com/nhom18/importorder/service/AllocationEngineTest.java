package com.nhom18.importorder.service;

import static org.junit.jupiter.api.Assertions.*;

import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.model.entity.SiteInventory;
import com.nhom18.importorder.model.enums.DeliveryMethod;
import com.nhom18.importorder.service.AllocationEngine.AllocationDetail;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AllocationEngineTest {

    private AllocationEngine engine;
    private List<Site> sites;
    private LocalDate currentDate;

    @BeforeEach
    public void setUp() {
        engine = new AllocationEngine();
        currentDate = LocalDate.of(2026, 5, 21);

        // Khởi tạo các Site mẫu tương tự như Seed Data
        sites = new ArrayList<>();
        sites.add(new Site("S_TOK", "Tokyo Site", 15, 3, "Japan", true));
        sites.add(new Site("S_SEO", "Seoul Site", 12, 2, "Korea", true));
        sites.add(new Site("S_SIN", "Singapore Site", 7, 1, "Singapore", true));
    }

    @Test
    public void testSuccessfulShipAllocation() {
        // Ngày mong muốn là 10 ngày sau (hiện tại + 10).
        // Chỉ có Singapore Site (SHIP 7 ngày, AIR 1 ngày) đáp ứng được bằng đường SHIP.
        // Seoul Site (SHIP 12 ngày) và Tokyo Site (SHIP 15 ngày) chỉ có thể bằng đường AIR.
        // Do ưu tiên SHIP > AIR, Singapore SHIP sẽ được chọn trước tiên.
        
        ImportRequestItem item = new ImportRequestItem();
        item.setMerchandiseCode("M_CPU_I7");
        item.setQuantityOrdered(10);
        item.setUnit("Cái");
        item.setDesiredDeliveryDate(currentDate.plusDays(10)); // 2026-05-31

        List<SiteInventory> inventories = new ArrayList<>();
        inventories.add(new SiteInventory("S_SIN", "M_CPU_I7", 10, "Cái")); // Singapore có 10 cái
        inventories.add(new SiteInventory("S_SEO", "M_CPU_I7", 20, "Cái")); // Seoul có 20 cái

        List<AllocationDetail> results = engine.allocate(item, currentDate, inventories, sites);

        assertEquals(1, results.size(), "Nên gom toàn bộ hàng từ 1 phương án khả dụng");
        AllocationDetail detail = results.get(0);
        assertEquals("S_SIN", detail.getSite().getSiteCode());
        assertEquals(DeliveryMethod.SHIP, detail.getMethod(), "Nên chọn SHIP thay vì AIR");
        assertEquals(10, detail.getAllocatedQuantity());
    }

    @Test
    public void testSuccessfulAirAllocation() {
        // Ngày mong muốn giao rất gấp: 3 ngày sau.
        // Do SHIP cần ít nhất 7 ngày, nên tất cả phương án SHIP đều không khả dụng.
        // Hệ thống bắt buộc phải dùng AIR.
        // Singapore (AIR 1 ngày), Seoul (AIR 2 ngày), Tokyo (AIR 3 ngày) đều khả dụng AIR.
        // Singapore có 5 cái, Seoul có 20 cái.
        // Đặt mua 15 cái.
        // Theo tiêu chuẩn 2: Ưu tiên Site có lượng tồn kho lớn hơn (Seoul: 20 > Singapore: 5).
        // Nên hệ thống sẽ lấy trọn vẹn 15 cái từ Seoul AIR.
        
        ImportRequestItem item = new ImportRequestItem();
        item.setMerchandiseCode("M_GPU_RTX4070");
        item.setQuantityOrdered(15);
        item.setUnit("Cái");
        item.setDesiredDeliveryDate(currentDate.plusDays(3)); // 2026-05-24

        List<SiteInventory> inventories = new ArrayList<>();
        inventories.add(new SiteInventory("S_SIN", "M_GPU_RTX4070", 5, "Cái"));  // Ít tồn kho hơn
        inventories.add(new SiteInventory("S_SEO", "M_GPU_RTX4070", 20, "Cái")); // Nhiều tồn kho hơn

        List<AllocationDetail> results = engine.allocate(item, currentDate, inventories, sites);

        assertEquals(1, results.size());
        AllocationDetail detail = results.get(0);
        assertEquals("S_SEO", detail.getSite().getSiteCode(), "Nên ưu tiên site có tồn kho lớn hơn để gom gọn");
        assertEquals(DeliveryMethod.AIR, detail.getMethod(), "Bắt buộc vận chuyển AIR vì quá hạn SHIP");
        assertEquals(15, detail.getAllocatedQuantity());
    }

    @Test
    public void testGreedySiteSelectionOrderByStock() {
        // Ngày mong muốn thoải mái: 20 ngày sau. Tất cả SHIP đều khả dụng.
        // Yêu cầu: 120 cái RAM.
        // Tồn kho: Singapore có 10, Seoul có 50, Tokyo có 100.
        // Thuật toán sắp xếp tồn kho giảm dần: Tokyo (100) > Seoul (50) > Singapore (10).
        // Phân bổ tối ưu: Lấy 100 từ Tokyo SHIP, lấy tiếp 20 từ Seoul SHIP. Singapore không cần lấy.
        
        ImportRequestItem item = new ImportRequestItem();
        item.setMerchandiseCode("M_RAM_16G");
        item.setQuantityOrdered(120);
        item.setUnit("Thanh");
        item.setDesiredDeliveryDate(currentDate.plusDays(20)); // 2026-06-10

        List<SiteInventory> inventories = new ArrayList<>();
        inventories.add(new SiteInventory("S_SIN", "M_RAM_16G", 10, "Thanh"));
        inventories.add(new SiteInventory("S_SEO", "M_RAM_16G", 50, "Thanh"));
        inventories.add(new SiteInventory("S_TOK", "M_RAM_16G", 100, "Thanh"));

        List<AllocationDetail> results = engine.allocate(item, currentDate, inventories, sites);

        assertEquals(2, results.size(), "Nên gom từ 2 site");
        
        AllocationDetail detail1 = results.get(0);
        assertEquals("S_TOK", detail1.getSite().getSiteCode(), "Ưu tiên site lớn nhất trước");
        assertEquals(100, detail1.getAllocatedQuantity());
        assertEquals(DeliveryMethod.SHIP, detail1.getMethod());

        AllocationDetail detail2 = results.get(1);
        assertEquals("S_SEO", detail2.getSite().getSiteCode(), "Lấy phần còn lại từ site lớn nhì");
        assertEquals(20, detail2.getAllocatedQuantity());
        assertEquals(DeliveryMethod.SHIP, detail2.getMethod());
    }

    @Test
    public void testInsufficientStockException() {
        // Tổng tồn kho toàn hệ thống nhỏ hơn nhu cầu hoặc thời hạn giao quá gấp.
        // Hệ thống phải ném ra Ngoại lệ thông báo lỗi thay vì tạo đơn hàng lỗi.
        
        ImportRequestItem item = new ImportRequestItem();
        item.setMerchandiseCode("M_SSD_1T");
        item.setQuantityOrdered(50);
        item.setUnit("Cái");
        item.setDesiredDeliveryDate(currentDate.plusDays(10));

        List<SiteInventory> inventories = new ArrayList<>();
        inventories.add(new SiteInventory("S_SIN", "M_SSD_1T", 10, "Cái"));
        inventories.add(new SiteInventory("S_SEO", "M_SSD_1T", 15, "Cái")); // Tổng tồn kho = 25 < 50

        assertThrows(IllegalArgumentException.class, () -> {
            engine.allocate(item, currentDate, inventories, sites);
        }, "Nên báo lỗi khi thiếu hụt tồn kho");
    }

    @Test
    public void testDesiredDateTooShortException() {
        // Ngày mong muốn giao hàng = ngày hiện tại (hoặc ngắn hơn air_days của Singapore là 1 ngày).
        // Sẽ không có phương án vận chuyển nào đáp ứng kịp. Báo lỗi trễ hạn.
        
        ImportRequestItem item = new ImportRequestItem();
        item.setMerchandiseCode("M_CPU_I7");
        item.setQuantityOrdered(5);
        item.setUnit("Cái");
        item.setDesiredDeliveryDate(currentDate); // 0 ngày chuẩn bị (nhanh nhất AIR Singapore cần 1 ngày)

        List<SiteInventory> inventories = new ArrayList<>();
        inventories.add(new SiteInventory("S_SIN", "M_CPU_I7", 10, "Cái"));

        assertThrows(IllegalArgumentException.class, () -> {
            engine.allocate(item, currentDate, inventories, sites);
        }, "Nên báo lỗi khi ngày giao hàng quá gấp không kịp vận chuyển");
    }
}
