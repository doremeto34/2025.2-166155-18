-- Bảng người dùng và phân quyền
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    full_name TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('BPBH', 'BPDHQT', 'SITE', 'BPQLK', 'ADMIN')),
    site_code TEXT,
    active INTEGER DEFAULT 1 CHECK (active IN (0, 1))
);

-- Bảng danh mục mặt hàng
CREATE TABLE IF NOT EXISTS merchandise (
    merchandise_code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    unit TEXT NOT NULL,
    price REAL NOT NULL CHECK (price >= 0),
    active INTEGER DEFAULT 1 CHECK (active IN (0, 1))
);

-- Bảng thông tin Site nhập khẩu
CREATE TABLE IF NOT EXISTS sites (
    site_code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    ship_days INTEGER NOT NULL CHECK (ship_days >= 0),
    air_days INTEGER NOT NULL CHECK (air_days >= 0),
    other_info TEXT,
    active INTEGER DEFAULT 1 CHECK (active IN (0, 1))
);

-- Bảng tồn kho tại từng Site
CREATE TABLE IF NOT EXISTS site_inventory (
    site_code TEXT NOT NULL,
    merchandise_code TEXT NOT NULL,
    in_stock_quantity INTEGER NOT NULL CHECK (in_stock_quantity >= 0),
    unit TEXT NOT NULL,
    PRIMARY KEY (site_code, merchandise_code),
    FOREIGN KEY (site_code) REFERENCES sites(site_code) ON DELETE CASCADE,
    FOREIGN KEY (merchandise_code) REFERENCES merchandise(merchandise_code) ON DELETE CASCADE
);

-- Bảng tồn kho nội bộ công ty (để thực hiện MRP logic)
CREATE TABLE IF NOT EXISTS company_inventory (
    merchandise_code TEXT PRIMARY KEY,
    in_stock_quantity INTEGER NOT NULL CHECK (in_stock_quantity >= 0),
    unit TEXT NOT NULL,
    FOREIGN KEY (merchandise_code) REFERENCES merchandise(merchandise_code) ON DELETE CASCADE
);

-- Bảng Yêu cầu nhập hàng (BPBH tạo)
CREATE TABLE IF NOT EXISTS import_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_by INTEGER NOT NULL,
    created_date TEXT NOT NULL, -- YYYY-MM-DD
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'APPROVED', 'REJECTED')),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Bảng chi tiết mặt hàng trong Yêu cầu nhập hàng
CREATE TABLE IF NOT EXISTS import_request_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    request_id INTEGER NOT NULL,
    merchandise_code TEXT NOT NULL,
    quantity_ordered INTEGER NOT NULL CHECK (quantity_ordered > 0),
    quantity_shortage INTEGER NOT NULL CHECK (quantity_shortage >= 0),
    unit TEXT NOT NULL,
    desired_delivery_date TEXT NOT NULL, -- YYYY-MM-DD
    FOREIGN KEY (request_id) REFERENCES import_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (merchandise_code) REFERENCES merchandise(merchandise_code)
);

-- Bảng Đơn đặt hàng (BPĐHQT tạo gửi cho Site, độc lập không bắt buộc có request_id ở mức đơn hàng)
CREATE TABLE IF NOT EXISTS orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    site_code TEXT NOT NULL,
    delivery_method TEXT NOT NULL CHECK (delivery_method IN ('SHIP', 'AIR')),
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    created_date TEXT NOT NULL,    -- YYYY-MM-DD
    estimated_arrival TEXT NOT NULL, -- YYYY-MM-DD
    cancel_reason TEXT,
    FOREIGN KEY (site_code) REFERENCES sites(site_code)
);

-- Bảng chi tiết mặt hàng trong Đơn đặt hàng
CREATE TABLE IF NOT EXISTS order_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    merchandise_code TEXT NOT NULL,
    quantity_ordered INTEGER NOT NULL CHECK (quantity_ordered > 0),
    quantity_confirmed INTEGER DEFAULT 0 CHECK (quantity_confirmed >= 0),
    quantity_received INTEGER DEFAULT 0 CHECK (quantity_received >= 0),
    unit TEXT NOT NULL,
    source_request_item_id INTEGER, -- Khóa ngoại liên kết dòng yêu cầu của BPBH (nếu có, Nullable nếu mua tự do)
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (merchandise_code) REFERENCES merchandise(merchandise_code),
    FOREIGN KEY (source_request_item_id) REFERENCES import_request_items(id) ON DELETE SET NULL
);

-- Bảng Phiếu nhập kho (BPQLK tạo khi nhận hàng)
CREATE TABLE IF NOT EXISTS warehouse_receipts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    confirmed_by INTEGER NOT NULL,
    confirm_date TEXT NOT NULL, -- YYYY-MM-DD
    notes TEXT,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (confirmed_by) REFERENCES users(id)
);

-- NẠP DỮ LIỆU MẪU (SEED DATA CHUẨN)
-- Mật khẩu mặc định cho tất cả các tài khoản là: "123456"
-- Đã được mã hóa SHA-256: "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92"

INSERT OR IGNORE INTO users (id, username, password_hash, full_name, role, site_code, active) VALUES
(1, 'dung_bpbh', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Nguyễn Trí Dũng', 'BPBH', NULL, 1),
(2, 'hung_bpdh', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Đỗ Thành Hưng', 'BPDHQT', NULL, 1),
(3, 'tung_bpdh', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Lê Hoàng Tùng', 'BPDHQT', NULL, 1),
(4, 'minh_bpdh', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Nguyễn Vũ Minh', 'BPDHQT', NULL, 1),
(5, 'site_tokyo', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Đại diện Site Tokyo', 'SITE', 'S_TOK', 1),
(6, 'site_seoul', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Đại diện Site Seoul', 'SITE', 'S_SEO', 1),
(7, 'site_sing', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Đại diện Site Singapore', 'SITE', 'S_SIN', 1),
(8, 'thai_bpqlk', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Nguyễn Quang Thái', 'BPQLK', NULL, 1),
(9, 'admin_sys', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Quản trị viên Hệ thống', 'ADMIN', NULL, 1);

-- Thêm các Site mẫu
INSERT OR IGNORE INTO sites (site_code, name, ship_days, air_days, other_info, active) VALUES
('S_TOK', 'Tokyo Import Site (Japan)', 15, 3, 'Đối tác Nhật Bản, uy tín cao', 1),
('S_SEO', 'Seoul Import Site (South Korea)', 12, 2, 'Đối tác Hàn Quốc, giá tốt', 1),
('S_SIN', 'Singapore Cargo Hub', 7, 1, 'Hub trung chuyển lớn nhất Đông Nam Á', 1);

-- Thêm mặt hàng kinh doanh
INSERT OR IGNORE INTO merchandise (merchandise_code, name, description, unit, price, active) VALUES
('M_CPU_I7', 'Intel Core i7 13700K', 'Bộ vi xử lý thế hệ 13', 'Cái', 420.0, 1),
('M_GPU_RTX4070', 'NVIDIA GeForce RTX 4070', 'Card đồ họa cao cấp', 'Cái', 650.0, 1),
('M_RAM_16G', 'Corsair Vengeance DDR5 16GB', 'Bộ nhớ trong hiệu năng cao', 'Thanh', 75.0, 1),
('M_SSD_1T', 'Samsung 990 Pro 1TB NVMe', 'Ổ cứng thể rắn tốc độ cực cao', 'Cái', 110.0, 1),
('M_CPU_I5', 'Intel Core i5 13400F', 'Bộ vi xử lý tầm trung thế hệ 13', 'Cái', 210.0, 1),
('M_CPU_I9', 'Intel Core i9 14900K', 'Bộ vi xử lý siêu cấp thế hệ 14', 'Cái', 590.0, 1),
('M_GPU_RTX4060', 'ASUS Dual RTX 4060 8GB', 'Card đồ họa hiệu năng/giá tốt', 'Cái', 320.0, 1),
('M_GPU_RTX4090', 'MSI Suprim X RTX 4090 24GB', 'Card đồ họa đỉnh cao gaming/AI', 'Cái', 1850.0, 1),
('M_RAM_8G', 'Kingston Fury Beast 8GB DDR4', 'Bộ nhớ trong tiêu chuẩn DDR4', 'Thanh', 35.0, 1),
('M_RAM_32G', 'G.Skill Trident Z5 32GB DDR5', 'Bộ nhớ trong DDR5 hiệu năng cao', 'Thanh', 145.0, 1),
('M_SSD_500G', 'Crucial P3 Plus 500GB NVMe', 'Ổ cứng SSD tầm trung bền bỉ', 'Cái', 55.0, 1),
('M_SSD_2T', 'WD Black SN850X 2TB NVMe', 'Ổ cứng SSD hiệu năng gaming', 'Cái', 180.0, 1),
('M_MAIN_B760', 'MSI MAG B760M Mortar Wifi', 'Bo mạch chủ tầm trung ổn định', 'Cái', 140.0, 1),
('M_MAIN_Z790', 'ASUS ROG Strix Z790-F Gaming', 'Bo mạch chủ cao cấp Intel', 'Cái', 310.0, 1),
('M_PSU_650W', 'Corsair CX650M 80 Plus Bronze', 'Nguồn máy tính công suất thực 650W', 'Cái', 65.0, 1),
('M_PSU_1000W', 'Seasonic Focus 1000W Gold', 'Nguồn máy tính cao cấp 1000W Gold', 'Cái', 195.0, 1),
('M_CASE_ATX', 'NZXT H5 Flow Black', 'Vỏ máy tính mid-tower tối ưu gió', 'Cái', 85.0, 1),
('M_COOLER_AIR', 'Noctua NH-D15 chromax.black', 'Tản nhiệt khí hiệu năng đỉnh cao', 'Cái', 105.0, 1),
('M_COOLER_AIO', 'Corsair iCUE H150i Elite Capellix', 'Tản nhiệt nước AIO 360mm RGB', 'Cái', 175.0, 1),
('M_MONITOR_24', 'Dell UltraSharp U2424H IPS 100Hz', 'Màn hình chuyên thiết kế đồ họa 24"', 'Cái', 220.0, 1),
('M_MONITOR_27', 'LG UltraGear 27GP850 QHD 180Hz', 'Màn hình chuyên gaming 2K IPS 27"', 'Cái', 340.0, 1),
('M_KEYBOARD', 'Keychron K8 Pro QMK Wireless', 'Bàn phím cơ không dây TKL hotswap', 'Cái', 110.0, 1),
('M_MOUSE', 'Logitech G Pro X Superlight Wireless', 'Chuột gaming không dây siêu nhẹ', 'Cái', 130.0, 1),
('M_HEADSET', 'HyperX Cloud III Gaming Headset', 'Tai nghe chơi game chuyên nghiệp 7.1', 'Cái', 90.0, 1);

-- Thêm tồn kho thực tế tại các Site
INSERT OR IGNORE INTO site_inventory (site_code, merchandise_code, in_stock_quantity, unit) VALUES
('S_SIN', 'M_CPU_I7', 10, 'Cái'),
('S_SIN', 'M_GPU_RTX4070', 5, 'Cái'),
('S_SIN', 'M_RAM_16G', 30, 'Thanh'),
('S_SIN', 'M_SSD_1T', 25, 'Cái'),
('S_SIN', 'M_CPU_I5', 40, 'Cái'),
('S_SIN', 'M_CPU_I9', 8, 'Cái'),
('S_SIN', 'M_GPU_RTX4060', 15, 'Cái'),
('S_SIN', 'M_GPU_RTX4090', 2, 'Cái'),
('S_SIN', 'M_RAM_8G', 50, 'Thanh'),
('S_SIN', 'M_RAM_32G', 12, 'Thanh'),
('S_SIN', 'M_SSD_500G', 60, 'Cái'),
('S_SIN', 'M_SSD_2T', 10, 'Cái'),
('S_SIN', 'M_MAIN_B760', 20, 'Cái'),
('S_SIN', 'M_MAIN_Z790', 5, 'Cái'),
('S_SIN', 'M_PSU_650W', 30, 'Cái'),
('S_SIN', 'M_PSU_1000W', 10, 'Cái'),
('S_SIN', 'M_CASE_ATX', 15, 'Cái'),
('S_SIN', 'M_COOLER_AIR', 12, 'Cái'),
('S_SIN', 'M_COOLER_AIO', 8, 'Cái'),
('S_SIN', 'M_MONITOR_24', 18, 'Cái'),
('S_SIN', 'M_MONITOR_27', 10, 'Cái'),
('S_SIN', 'M_KEYBOARD', 25, 'Cái'),
('S_SIN', 'M_MOUSE', 20, 'Cái'),
('S_SIN', 'M_HEADSET', 30, 'Cái'),

('S_SEO', 'M_CPU_I7', 50, 'Cái'),
('S_SEO', 'M_GPU_RTX4070', 20, 'Cái'),
('S_SEO', 'M_RAM_16G', 100, 'Thanh'),
('S_SEO', 'M_SSD_1T', 80, 'Cái'),
('S_SEO', 'M_CPU_I5', 120, 'Cái'),
('S_SEO', 'M_CPU_I9', 25, 'Cái'),
('S_SEO', 'M_GPU_RTX4060', 45, 'Cái'),
('S_SEO', 'M_GPU_RTX4090', 10, 'Cái'),
('S_SEO', 'M_RAM_8G', 150, 'Thanh'),
('S_SEO', 'M_RAM_32G', 40, 'Thanh'),
('S_SEO', 'M_SSD_500G', 180, 'Cái'),
('S_SEO', 'M_SSD_2T', 35, 'Cái'),
('S_SEO', 'M_MAIN_B760', 60, 'Cái'),
('S_SEO', 'M_MAIN_Z790', 20, 'Cái'),
('S_SEO', 'M_PSU_650W', 90, 'Cái'),
('S_SEO', 'M_PSU_1000W', 30, 'Cái'),
('S_SEO', 'M_CASE_ATX', 50, 'Cái'),
('S_SEO', 'M_COOLER_AIR', 35, 'Cái'),
('S_SEO', 'M_COOLER_AIO', 25, 'Cái'),
('S_SEO', 'M_MONITOR_24', 40, 'Cái'),
('S_SEO', 'M_MONITOR_27', 30, 'Cái'),
('S_SEO', 'M_KEYBOARD', 70, 'Cái'),
('S_SEO', 'M_MOUSE', 60, 'Cái'),
('S_SEO', 'M_HEADSET', 80, 'Cái'),

('S_TOK', 'M_CPU_I7', 150, 'Cái'),
('S_TOK', 'M_GPU_RTX4070', 80, 'Cái'),
('S_TOK', 'M_RAM_16G', 300, 'Thanh'),
('S_TOK', 'M_SSD_1T', 200, 'Cái'),
('S_TOK', 'M_CPU_I5', 300, 'Cái'),
('S_TOK', 'M_CPU_I9', 70, 'Cái'),
('S_TOK', 'M_GPU_RTX4060', 150, 'Cái'),
('S_TOK', 'M_GPU_RTX4090', 25, 'Cái'),
('S_TOK', 'M_RAM_8G', 400, 'Thanh'),
('S_TOK', 'M_RAM_32G', 120, 'Thanh'),
('S_TOK', 'M_SSD_500G', 500, 'Cái'),
('S_TOK', 'M_SSD_2T', 110, 'Cái'),
('S_TOK', 'M_MAIN_B760', 180, 'Cái'),
('S_TOK', 'M_MAIN_Z790', 65, 'Cái'),
('S_TOK', 'M_PSU_650W', 250, 'Cái'),
('S_TOK', 'M_PSU_1000W', 90, 'Cái'),
('S_TOK', 'M_CASE_ATX', 120, 'Cái'),
('S_TOK', 'M_COOLER_AIR', 90, 'Cái'),
('S_TOK', 'M_COOLER_AIO', 80, 'Cái'),
('S_TOK', 'M_MONITOR_24', 150, 'Cái'),
('S_TOK', 'M_MONITOR_27', 95, 'Cái'),
('S_TOK', 'M_KEYBOARD', 200, 'Cái'),
('S_TOK', 'M_MOUSE', 180, 'Cái'),
('S_TOK', 'M_HEADSET', 220, 'Cái');

-- Thêm tồn kho thực tế tại kho nội bộ của công ty (để chạy MRP logic)
INSERT OR IGNORE INTO company_inventory (merchandise_code, in_stock_quantity, unit) VALUES
('M_CPU_I7', 15, 'Cái'),
('M_GPU_RTX4070', 3, 'Cái'),
('M_RAM_16G', 20, 'Thanh'),
('M_SSD_1T', 10, 'Cái'),
('M_CPU_I5', 5, 'Cái'),
('M_CPU_I9', 0, 'Cái'),
('M_GPU_RTX4060', 4, 'Cái'),
('M_GPU_RTX4090', 0, 'Cái'),
('M_RAM_8G', 25, 'Thanh'),
('M_RAM_32G', 2, 'Thanh'),
('M_SSD_500G', 15, 'Cái'),
('M_SSD_2T', 2, 'Cái'),
('M_MAIN_B760', 6, 'Cái'),
('M_MAIN_Z790', 1, 'Cái'),
('M_PSU_650W', 10, 'Cái'),
('M_PSU_1000W', 2, 'Cái'),
('M_CASE_ATX', 5, 'Cái'),
('M_COOLER_AIR', 3, 'Cái'),
('M_COOLER_AIO', 1, 'Cái'),
('M_MONITOR_24', 4, 'Cái'),
('M_MONITOR_27', 2, 'Cái'),
('M_KEYBOARD', 8, 'Cái'),
('M_MOUSE', 10, 'Cái'),
('M_HEADSET', 6, 'Cái');
