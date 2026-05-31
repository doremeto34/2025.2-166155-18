package com.nhom18.importorder.service;

import com.nhom18.importorder.dao.IImportRequestDAO;
import com.nhom18.importorder.dao.IOrderDAO;
import com.nhom18.importorder.dao.ISiteDAO;
import com.nhom18.importorder.dao.ISiteInventoryDAO;
import com.nhom18.importorder.dao.impl.SQLiteImportRequestDAO;
import com.nhom18.importorder.dao.impl.SQLiteOrderDAO;
import com.nhom18.importorder.dao.impl.SQLiteSiteDAO;
import com.nhom18.importorder.dao.impl.SQLiteSiteInventoryDAO;
import com.nhom18.importorder.model.entity.ImportRequest;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.model.entity.SiteInventory;
import com.nhom18.importorder.model.entity.User;
import com.nhom18.importorder.model.enums.DeliveryMethod;
import com.nhom18.importorder.model.enums.OrderStatus;
import com.nhom18.importorder.model.enums.RequestStatus;
import com.nhom18.importorder.service.AllocationEngine.AllocationDetail;
import com.nhom18.importorder.util.SessionManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderService {
    private final IOrderDAO orderDAO;
    private final IImportRequestDAO requestDAO;
    private final ISiteInventoryDAO siteInventoryDAO;
    private final ISiteDAO siteDAO;
    private final AllocationEngine allocationEngine;

    public OrderService() {
        this.orderDAO = new SQLiteOrderDAO();
        this.requestDAO = new SQLiteImportRequestDAO();
        this.siteInventoryDAO = new SQLiteSiteInventoryDAO();
        this.siteDAO = new SQLiteSiteDAO();
        this.allocationEngine = new AllocationEngine();
    }

    // Constructor dành cho Unit Tests
    public OrderService(IOrderDAO orderDAO, IImportRequestDAO requestDAO, ISiteInventoryDAO siteInventoryDAO, ISiteDAO siteDAO, AllocationEngine allocationEngine) {
        this.orderDAO = orderDAO;
        this.requestDAO = requestDAO;
        this.siteInventoryDAO = siteInventoryDAO;
        this.siteDAO = siteDAO;
        this.allocationEngine = allocationEngine;
    }

    public List<Order> getAllOrders() {
        return orderDAO.getAll();
    }

    public List<Order> getOrdersBySite(String siteCode) {
        return orderDAO.getBySiteCode(siteCode);
    }

    public List<Order> getOrdersByRequest(int requestId) {
        return orderDAO.getByRequestId(requestId);
    }

    /**
     * Dựa trên mã yêu cầu nhập khẩu, chạy thuật toán phân bổ để xuất ra danh sách đơn hàng ĐỀ XUẤT.
     * Bảng đề xuất chưa lưu vào CSDL, người dùng có thể review trước ở UI.
     */
    public List<Order> generateProposedOrders(int requestId) {
        ImportRequest request = requestDAO.getById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Không tìm thấy yêu cầu nhập hàng có mã ID: " + requestId);
        }

        List<Site> allSites = siteDAO.getAllActive();
        List<Order> proposedOrders = new ArrayList<>();

        // Bản đồ gom nhóm các dòng mặt hàng đề xuất theo (siteCode + "_" + deliveryMethod)
        Map<String, Order> orderGroups = new HashMap<>();

        LocalDate currentDate = LocalDate.now();

        for (ImportRequestItem item : request.getItems()) {
            List<SiteInventory> inventories = siteInventoryDAO.getByMerchandiseCode(item.getMerchandiseCode());
            
            // Chạy phân bổ tối ưu cho mặt hàng này
            List<AllocationDetail> details = allocationEngine.allocate(item, currentDate, inventories, allSites);

            // Gom nhóm kết quả phân bổ vào các đơn hàng đề xuất tương ứng
            for (AllocationDetail detail : details) {
                String key = detail.getSite().getSiteCode() + "_" + detail.getMethod().name();
                Order order = orderGroups.get(key);
                if (order == null) {
                    order = new Order();
                    order.setRequestId(requestId);
                    order.setSiteCode(detail.getSite().getSiteCode());
                    order.setSiteName(detail.getSite().getName());
                    order.setDeliveryMethod(detail.getMethod());
                    order.setStatus(OrderStatus.PENDING);
                    order.setCreatedDate(currentDate);
                    // Ngày đến dự kiến là ngày xa nhất trong số các dòng hàng của đơn hàng này
                    order.setEstimatedArrival(detail.getEstimatedArrivalDate());
                    
                    orderGroups.put(key, order);
                }

                // Cập nhật ngày đến dự kiến (lấy ngày đến trễ nhất để làm đại diện cho toàn đơn hàng)
                if (detail.getEstimatedArrivalDate().isAfter(order.getEstimatedArrival())) {
                    order.setEstimatedArrival(detail.getEstimatedArrivalDate());
                }

                // Tạo dòng mặt hàng cho đơn hàng đề xuất
                OrderItem orderItem = new OrderItem();
                orderItem.setMerchandiseCode(item.getMerchandiseCode());
                orderItem.setMerchandiseName(item.getMerchandiseName());
                orderItem.setQuantityOrdered(detail.getAllocatedQuantity());
                orderItem.setQuantityConfirmed(0);
                orderItem.setQuantityReceived(0);
                orderItem.setUnit(item.getUnit());

                order.addItem(orderItem);
            }
        }

        return new ArrayList<>(orderGroups.values());
    }

    /**
     * Xác nhận và lưu chính thức danh sách đơn hàng được phân bổ vào database.
     * Đồng thời thực hiện trừ tồn kho ảo tại các Site tương ứng để tránh bị đặt quá dung lượng.
     */
    public void confirmAndSaveOrders(int requestId, List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            throw new IllegalArgumentException("Danh sách đơn hàng phê duyệt rỗng.");
        }

        // 1. Lưu từng đơn hàng và chi tiết đơn hàng
        for (Order order : orders) {
            orderDAO.insert(order);

            // 2. Trừ tồn kho tương ứng tại các Site
            for (OrderItem item : order.getItems()) {
                SiteInventory inventory = siteInventoryDAO.get(order.getSiteCode(), item.getMerchandiseCode());
                if (inventory != null) {
                    int newQty = Math.max(0, inventory.getInStockQuantity() - item.getQuantityOrdered());
                    siteInventoryDAO.updateStock(order.getSiteCode(), item.getMerchandiseCode(), newQty);
                }
            }
        }

        // 3. Cập nhật trạng thái Yêu cầu nhập khẩu sang APPROVED (đã duyệt phân bổ)
        requestDAO.updateStatus(requestId, RequestStatus.APPROVED);
    }

    /**
     * Hủy đơn hàng (ví dụ: bị Site từ chối). Giải phóng lượng tồn kho đã trừ tạm tính.
     */
    public void handleCancelledOrder(int orderId, String reason) {
        Order order = orderDAO.getById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng cần hủy có mã: " + orderId);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return; // Đã hủy rồi thì thôi
        }

        // 1. Cập nhật trạng thái đơn hàng sang CANCELLED kèm lý do
        orderDAO.updateCancelReason(orderId, reason);

        // 2. Cộng trả lại tồn kho khả dụng cho Site và phục hồi shortage yêu cầu BPBH
        for (OrderItem item : order.getItems()) {
            SiteInventory inventory = siteInventoryDAO.get(order.getSiteCode(), item.getMerchandiseCode());
            if (inventory != null) {
                int newQty = inventory.getInStockQuantity() + item.getQuantityOrdered();
                siteInventoryDAO.updateStock(order.getSiteCode(), item.getMerchandiseCode(), newQty);
            }
            
            // Khôi phục shortage của BPBH nếu có liên kết
            if (item.getSourceRequestItemId() != null) {
                requestDAO.adjustShortageQuantity(item.getSourceRequestItemId(), item.getQuantityOrdered());
                System.out.println("MRP: Đã hoàn trả shortage của request item #" + item.getSourceRequestItemId() + " đi +" + item.getQuantityOrdered());
            }
        }
    }

    /**
     * Cập nhật trạng thái giao hàng / phê duyệt đơn hàng của Site (ví dụ: CONFIRMED hoặc SHIPPED).
     */
    public void updateOrderShipmentStatus(int orderId, OrderStatus status) {
        Order order = orderDAO.getById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId);
        }
        orderDAO.updateStatus(orderId, status);
        
        // Nếu xác nhận đơn hàng, tự động cập nhật số lượng xác nhận của tất cả mặt hàng bằng số lượng đặt
        if (status == OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getItems()) {
                orderDAO.updateItemQuantities(item.getId(), item.getQuantityOrdered(), item.getQuantityReceived());
            }
        }
    }

    /**
     * Tái phân bổ đơn hàng đã bị hủy sang một hoặc nhiều Site khác khả dụng (loại trừ Site cũ đã hủy).
     */
    public void reallocateCancelledOrder(int orderId) {
        Order cancelledOrder = orderDAO.getById(orderId);
        if (cancelledOrder == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng cần tái phân bổ: " + orderId);
        }
        if (cancelledOrder.getStatus() != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Chỉ có thể tái phân bổ đơn hàng có trạng thái BỊ HỦY (CANCELLED)!");
        }
        if (cancelledOrder.getCancelReason() != null && cancelledOrder.getCancelReason().contains("[REALLOCATED]")) {
            throw new IllegalArgumentException("Đơn hàng này đã được tái phân bổ trước đó!");
        }

        ImportRequest request = requestDAO.getById(cancelledOrder.getRequestId());
        
        // Lấy tất cả các Site hoạt động trừ Site đã từ chối đơn hàng này
        List<Site> allSites = siteDAO.getAllActive();
        List<Site> filteredSites = new ArrayList<>();
        for (Site s : allSites) {
            if (!s.getSiteCode().equals(cancelledOrder.getSiteCode())) {
                filteredSites.add(s);
            }
        }

        Map<String, Order> orderGroups = new HashMap<>();
        LocalDate currentDate = LocalDate.now();

        // Duyệt qua từng mặt hàng trong đơn hàng cũ và phân bổ lại
        for (OrderItem orderItem : cancelledOrder.getItems()) {
            List<SiteInventory> inventories = siteInventoryDAO.getByMerchandiseCode(orderItem.getMerchandiseCode());
            
            // Xây dựng thông tin ImportRequestItem tạm thời để đưa vào AllocationEngine
            ImportRequestItem tempItem = new ImportRequestItem();
            tempItem.setMerchandiseCode(orderItem.getMerchandiseCode());
            tempItem.setQuantityOrdered(orderItem.getQuantityOrdered());
            tempItem.setUnit(orderItem.getUnit());
            tempItem.setMerchandiseName(orderItem.getMerchandiseName());
            
            // Lấy ngày giao hàng mong muốn từ ImportRequest gốc nếu có, nếu không thì lấy ngày đến của đơn cũ
            LocalDate desiredDate = cancelledOrder.getEstimatedArrival();
            if (request != null) {
                for (ImportRequestItem reqItem : request.getItems()) {
                    if (reqItem.getMerchandiseCode().equals(orderItem.getMerchandiseCode())) {
                        desiredDate = reqItem.getDesiredDeliveryDate();
                        break;
                    }
                }
            }
            tempItem.setDesiredDeliveryDate(desiredDate);

            // Chạy phân bổ cho mặt hàng này trên các Site còn lại
            List<AllocationDetail> details = allocationEngine.allocate(tempItem, currentDate, inventories, filteredSites);

            // Gom nhóm kết quả phân bổ vào các đơn hàng mới
            for (AllocationDetail detail : details) {
                String key = detail.getSite().getSiteCode() + "_" + detail.getMethod().name();
                Order order = orderGroups.get(key);
                if (order == null) {
                    order = new Order();
                    order.setRequestId(cancelledOrder.getRequestId());
                    order.setSiteCode(detail.getSite().getSiteCode());
                    order.setSiteName(detail.getSite().getName());
                    order.setDeliveryMethod(detail.getMethod());
                    order.setStatus(OrderStatus.PENDING);
                    order.setCreatedDate(currentDate);
                    order.setEstimatedArrival(detail.getEstimatedArrivalDate());
                    orderGroups.put(key, order);
                }

                if (detail.getEstimatedArrivalDate().isAfter(order.getEstimatedArrival())) {
                    order.setEstimatedArrival(detail.getEstimatedArrivalDate());
                }

                OrderItem newOrderItem = new OrderItem();
                newOrderItem.setMerchandiseCode(orderItem.getMerchandiseCode());
                newOrderItem.setMerchandiseName(orderItem.getMerchandiseName());
                newOrderItem.setQuantityOrdered(detail.getAllocatedQuantity());
                newOrderItem.setQuantityConfirmed(0);
                newOrderItem.setQuantityReceived(0);
                newOrderItem.setUnit(orderItem.getUnit());

                order.addItem(newOrderItem);
            }
        }

        // Thực hiện lưu các đơn hàng mới và trừ tồn kho ảo tại các Site mới
        List<Order> createdOrders = new ArrayList<>(orderGroups.values());
        for (Order newOrder : createdOrders) {
            orderDAO.insert(newOrder);

            for (OrderItem item : newOrder.getItems()) {
                SiteInventory inventory = siteInventoryDAO.get(newOrder.getSiteCode(), item.getMerchandiseCode());
                if (inventory != null) {
                    int newQty = Math.max(0, inventory.getInStockQuantity() - item.getQuantityOrdered());
                    siteInventoryDAO.updateStock(newOrder.getSiteCode(), item.getMerchandiseCode(), newQty);
                }
            }
        }

        // Cập nhật lý do hủy của đơn hàng cũ để đánh dấu đã tái phân bổ thành công
        String newReason = (cancelledOrder.getCancelReason() != null ? cancelledOrder.getCancelReason() : "") + " [REALLOCATED]";
        orderDAO.updateCancelReason(orderId, newReason.trim());
    }

    public Order getOrderById(int orderId) {
        return orderDAO.getById(orderId);
    }

    /**
     * Chạy thuật toán phân bổ in-memory từ danh sách mặt hàng tự do chọn trên UI.
     */
    public List<Order> generateProposedOrders(List<ImportRequestItem> items) {
        List<Site> allSites = siteDAO.getAllActive();
        Map<String, Order> orderGroups = new HashMap<>();
        LocalDate currentDate = LocalDate.now();

        for (ImportRequestItem item : items) {
            List<SiteInventory> inventories = siteInventoryDAO.getByMerchandiseCode(item.getMerchandiseCode());
            
            // Chạy phân bổ tối ưu cho mặt hàng này
            List<AllocationDetail> details = allocationEngine.allocate(item, currentDate, inventories, allSites);

            // Gom nhóm kết quả phân bổ vào các đơn hàng đề xuất tương ứng
            for (AllocationDetail detail : details) {
                String key = detail.getSite().getSiteCode() + "_" + detail.getMethod().name();
                Order order = orderGroups.get(key);
                if (order == null) {
                    order = new Order();
                    order.setRequestId(-1); // ID tạm thời
                    order.setSiteCode(detail.getSite().getSiteCode());
                    order.setSiteName(detail.getSite().getName());
                    order.setDeliveryMethod(detail.getMethod());
                    order.setStatus(OrderStatus.PENDING);
                    order.setCreatedDate(currentDate);
                    order.setEstimatedArrival(detail.getEstimatedArrivalDate());
                    
                    orderGroups.put(key, order);
                }

                // Cập nhật ngày đến dự kiến của đơn hàng
                if (detail.getEstimatedArrivalDate().isAfter(order.getEstimatedArrival())) {
                    order.setEstimatedArrival(detail.getEstimatedArrivalDate());
                }

                // Tạo dòng mặt hàng
                OrderItem orderItem = new OrderItem();
                orderItem.setMerchandiseCode(item.getMerchandiseCode());
                orderItem.setMerchandiseName(item.getMerchandiseName());
                orderItem.setQuantityOrdered(detail.getAllocatedQuantity());
                orderItem.setQuantityConfirmed(0);
                orderItem.setQuantityReceived(0);
                orderItem.setUnit(item.getUnit());
                if (item.getId() > 0) {
                    orderItem.setSourceRequestItemId(item.getId());
                }

                order.addItem(orderItem);
            }
        }

        return new ArrayList<>(orderGroups.values());
    }

    /**
     * Lưu các đơn đặt hàng mới (Purchase Orders). 
     * Đồng thời:
     * - Khấu trừ tồn kho tại các Site tương ứng.
     * - Cập nhật lượng shortage trong import_request_items đối với các dòng có liên kết.
     */
    public void saveCustomFreeRequestAndOrders(LocalDate desiredDate, List<Order> customOrders) {
        if (customOrders == null || customOrders.isEmpty()) {
            throw new IllegalArgumentException("Danh sách đơn hàng rỗng!");
        }

        // 1. Tạo và lưu phiếu Yêu cầu nhập khẩu tự do (ImportRequest) đại diện
        ImportRequest request = new ImportRequest();
        User currentUser = SessionManager.getInstance().getCurrentUser();
        request.setCreatedBy(currentUser != null ? currentUser.getId() : 9); // Mặc định admin_sys (id = 9)
        request.setCreatedDate(LocalDate.now());
        request.setStatus(RequestStatus.APPROVED);

        // Gom các mặt hàng đặt mua từ tất cả các đơn hàng con
        Map<String, ImportRequestItem> requestItemMap = new HashMap<>();
        for (Order order : customOrders) {
            for (OrderItem oi : order.getItems()) {
                String code = oi.getMerchandiseCode();
                ImportRequestItem ri = requestItemMap.get(code);
                if (ri == null) {
                    ri = new ImportRequestItem();
                    ri.setMerchandiseCode(code);
                    ri.setMerchandiseName(oi.getMerchandiseName());
                    ri.setQuantityOrdered(0);
                    ri.setQuantityShortage(0);
                    ri.setUnit(oi.getUnit());
                    ri.setDesiredDeliveryDate(desiredDate);
                    requestItemMap.put(code, ri);
                }
                ri.setQuantityOrdered(ri.getQuantityOrdered() + oi.getQuantityOrdered());
            }
        }
        
        for (ImportRequestItem ri : requestItemMap.values()) {
            request.addItem(ri);
        }

        int requestId = requestDAO.insert(request);
        request.setId(requestId);
        
        // Lưu từng dòng item vào database
        for (ImportRequestItem ri : new ArrayList<>(request.getItems())) {
            ri.setRequestId(requestId);
            requestDAO.insertItem(ri);
        }

        // 2. Lưu từng đơn hàng và thực hiện khấu trừ tồn kho Site & cập nhật shortage của BPBH
        for (Order order : customOrders) {
            order.setRequestId(requestId); // Liên kết đơn hàng với yêu cầu nhập vừa tạo
            order.setCreatedDate(LocalDate.now());
            order.setStatus(OrderStatus.PENDING);

            // Lưu đơn hàng qua DAO (tự động lưu cả OrderItems)
            orderDAO.insert(order);

            // Xử lý từng dòng chi tiết đơn hàng
            for (OrderItem item : order.getItems()) {
                // Nếu dòng này liên kết với một yêu cầu mặt hàng của BPBH, ta trừ shortage của nó
                if (item.getSourceRequestItemId() != null) {
                    requestDAO.adjustShortageQuantity(item.getSourceRequestItemId(), -item.getQuantityOrdered());
                    System.out.println("MRP: Đã giảm shortage của request item #" + item.getSourceRequestItemId() + " đi -" + item.getQuantityOrdered());
                }

                // Khấu trừ tồn kho khả dụng tại Site đối tác tương ứng
                SiteInventory inventory = siteInventoryDAO.get(order.getSiteCode(), item.getMerchandiseCode());
                if (inventory != null) {
                    int newQty = Math.max(0, inventory.getInStockQuantity() - item.getQuantityOrdered());
                    siteInventoryDAO.updateStock(order.getSiteCode(), item.getMerchandiseCode(), newQty);
                }
            }
        }
    }
}
