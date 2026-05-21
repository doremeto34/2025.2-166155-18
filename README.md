# ⚡ HỆ THỐNG ĐẶT HÀNG NHẬP KHẨU (IMPORT ORDER MANAGEMENT SYSTEM)
 DỰ ÁN CUỐI KỲ BÀI TẬP LỚN - NHÓM 18

Hệ thống Quản lý Đặt hàng Nhập khẩu là một ứng dụng máy tính (Desktop Application) được xây dựng dựa trên công nghệ **Java 21**, **JavaFX 21** và **SQLite**, áp dụng nghiêm ngặt các nguyên lý thiết kế hướng đối tượng (SOLID, GRASP) và mô hình kiến trúc phân lớp **Layered MVC kết hợp DAO (Data Access Object)**.

Hệ thống hỗ trợ quy trình nghiệp vụ khép kín từ lập yêu cầu, chạy thuật toán phân bổ tối ưu tự động, lập đơn hàng quốc tế, tương tác với đối tác Site, tái phân bổ đơn hàng khi bị từ chối, và kiểm nhận nhập kho.

---

## 🚀 TÍNH NĂNG NỔI BẬT

### 1. Phân hệ Quản Lý Danh Mục Mặt Hàng (Bộ phận Bán hàng - BPBH)
*   **Giao diện SplitPane Master-Detail**: Hiển thị lưới sản phẩm ở trên, xem chi tiết đầy đủ ở dưới, tích hợp tìm kiếm thời gian thực và lọc theo trạng thái kinh doanh.
*   **Form Popup Dialog**: Hỗ trợ Thêm mới/Cập nhật thông tin sản phẩm, tự động khóa mã sản phẩm khi sửa thông tin để đảm bảo toàn vẹn.
*   **Ràng buộc an toàn**: Hệ thống chặn đứng hành vi ngừng kinh doanh của sản phẩm nếu sản phẩm đó đang tồn tại trong các yêu cầu nhập hàng dở dang chưa hoàn thành.

### 2. Phân hệ Xử Lý Yêu Cầu & Phân Bổ Tự Động (Bộ phận Điều phối - BPĐHQT)
*   **Quy trình 2 Tab thông minh**: Loại bỏ hoàn toàn sự chật chội bằng cách chia luồng xử lý thành 2 bước (Bước 1: Chọn yêu cầu & xem chi tiết mặt hàng; Bước 2: Xem kết quả phân bổ đề xuất & phê duyệt).
*   **Thuật toán Phân bổ Tối ưu (Allocation Engine)**: Áp dụng thuật toán tham lam (Greedy) tự động tìm kiếm phương án cung ứng từ các đối tác Site theo 3 tiêu chí ưu tiên:
    1. Tiết kiệm chi phí: Ưu tiên phương thức vận chuyển đường biển (**SHIP**) hơn đường hàng không (**AIR**).
    2. Gom gọn đơn hàng: Ưu tiên Site đối tác có lượng tồn kho dự phòng lớn hơn.
    3. Tối ưu thời gian: Ưu tiên Site đối tác có thời gian vận chuyển nhanh hơn.
*   **Cơ chế sửa lỗi Tái phân bổ (Anti-Double Reallocation)**: Khi một đơn hàng bị đối tác từ chối, điều phối viên có thể nhấn tái phân bổ để chạy lại thuật toán loại trừ đối tác đó. Hệ thống tự động đánh dấu vết `[REALLOCATED]` và vô hiệu hóa nút bấm tái phân bổ của đơn hàng cũ để ngăn chặn việc tạo trùng lặp đơn hàng thay thế.

### 3. Phân hệ Quản Lý Đối Tác Site (BPĐHQT)
*   **Quản trị thông tin Site**: Quản lý thông tin liên hệ, thời gian vận chuyển (AIR/SHIP) của từng đối tác.
*   **Ràng buộc nghiệp vụ**: Chặn đứng hành vi tắt hoạt động (Inactive) của Site nếu Site đó đang có đơn hàng dở dang chưa hoàn thành.
*   **Lịch sử cung ứng**: Cho phép xem toàn bộ danh sách đơn hàng mà Site đó đã thực hiện ngay tại panel chi tiết.

### 4. Phân hệ Đối Tác Site (SITE Role)
*   **Xem đơn hàng nhận được**: Site đối tác có thể theo dõi danh sách đơn hàng gửi riêng cho họ.
*   **Xác nhận / Từ chối cung cấp**: Hỗ trợ xác nhận đơn hàng hoặc từ chối cung cấp kèm theo popup bắt buộc nhập lý do từ chối cụ thể.
*   **Quản lý tồn kho**: Đại diện Site tự cập nhật lượng tồn kho thực tế của họ trên hệ thống.

### 5. Phân hệ Nhận Hàng & Kiểm Kho (Bộ phận Quản lý kho - BPQLK)
*   **Đồng bộ thực nhận**: Nhân viên kiểm kho đối chiếu song song số lượng đặt hàng và số lượng thực nhận tại cảng.
*   **Cảnh báo sai lệch**: Hệ thống tự động bôi đỏ hiển thị chênh lệch số lượng và bắt buộc nhập lý do sai lệch để lưu vết lịch sử trước khi nhập kho nội bộ.

### 6. Phân hệ Quản Trị Hệ Thống (ADMIN Role)
*   **Quản lý nhân viên**: CRUD tài khoản nhân viên phân quyền rõ ràng theo 5 nhóm vai trò (ADMIN, BPBH, BPĐHQT, SITE, BPQLK).
*   **Bảo mật tài khoản**: Băm mật khẩu an toàn bằng thuật toán SHA-256. Đặt lại mật khẩu nhanh chóng thông qua hộp thoại Popup Dialog.
*   **Ràng buộc tự bảo vệ**: Khóa nút vô hiệu hóa nếu chọn chính tài khoản ADMIN đang đăng nhập để ngăn chặn việc tự khóa tài khoản của mình.

---

## 🛠️ YÊU CẦU HỆ THỐNG
*   **Java Development Kit (JDK)**: Phiên bản **21** trở lên.
*   **Apache Maven**: Phiên bản **3.9** trở lên.
*   **Hệ điều hành**: Windows 10/11, macOS, hoặc Linux.

---

## 📥 HƯỚNG DẪN CÀI ĐẶT & KHỞI CHẠY

### 1. Tải mã nguồn về máy cục bộ
```bash
git clone https://github.com/doremeto34/2025.2-166155-18.git
cd "Import System"
```

### 2. Cài đặt các gói phụ thuộc (Dependencies)
Sử dụng Maven để tải và cài đặt các thư viện JavaFX, SQLite JDBC:
```bash
mvn clean install
```

### 3. Chạy các ca kiểm thử tự động (Unit Tests)
Hệ thống tích hợp 45 ca kiểm thử bảo phủ toàn bộ logic nghiệp vụ cốt lõi, bảo đảm độ tin cậy 100%:
```bash
mvn test
```

### 4. Khởi chạy ứng dụng (GUI JavaFX)
Chạy ứng dụng trực tiếp bằng lệnh:
```bash
mvn javafx:run
```

---

## 🔑 TÀI KHOẢN ĐĂNG NHẬP THỬ NGHIỆM
Để thuận tiện cho quá trình kiểm tra toàn bộ luồng nghiệp vụ liên thông (End-to-End), bạn có thể sử dụng các tài khoản demo đã được nạp sẵn dưới đây:

| Vai Trò | Tên Đăng Nhập | Mật Khẩu | Họ và Tên Nhân Viên | Ghi Chú |
| :--- | :--- | :--- | :--- | :--- |
| **ADMIN** | `admin_sys` | `123456` | Quản Trị Viên Hệ Thống | Toàn quyền cấu hình |
| **BPBH** (Bán hàng) | `dung_bpbh` | `123456` | Nguyễn Trí Dũng | Lập yêu cầu, xem mặt hàng |
| **BPĐHQT** (Đặt hàng) | `hung_bpdh` | `123456` | Lê Hoàng Hùng | Chạy phân bổ, Duyệt đơn, Tái phân bổ |
| **SITE** (Đối tác Seoul) | `site_seoul` | `123456` | Seoul Import Rep | Xác nhận/Từ chối đơn hàng |
| **SITE** (Đối tác Tokyo) | `site_tokyo` | `123456` | Tokyo Import Rep | Xác nhận/Từ chối đơn hàng |
| **BPQLK** (Thủ kho) | `thai_bpqlk` | `123456` | Trần Hồng Thái | Kiểm nhận thực tế, Nhập kho |

---

## 📂 CẤU TRÚC THƯ MỤC DỰ ÁN
Dự án được cấu trúc mạch lạc theo mô hình phân lớp chuẩn:
```text
import-order-system/
│
├── src/
│   ├── main/
│   │   ├── java/com/nhom18/importorder/
│   │   │   ├── App.java                   # Điểm khởi chạy ứng dụng (Main class)
│   │   │   ├── dao/                       # Tầng tương tác CSDL (SQLite JDBC)
│   │   │   │   ├── impl/                  # Implement chi tiết câu lệnh SQL
│   │   │   ├── model/                     # Thực thể nghiệp vụ (Entities & Enums)
│   │   │   ├── service/                   # Chứa logic nghiệp vụ & Thuật toán phân bổ
│   │   │   ├── controller/                # Tầng điều khiển giao diện (MVC Controllers)
│   │   │   └── util/                      # Các tiện ích (Băm mật khẩu, Điều hướng, Session)
│   │   │
│   │   └── resources/
│   │       ├── com/nhom18/importorder/view/ # Chứa các file giao diện FXML
│   │       ├── css/                       # Tệp styles.css quản lý theme màu cao cấp
│   │       └── db/                        # Chứa file script tạo bảng CSDL (schema.sql)
│   │
│   └── test/java/com/nhom18/importorder/  # Chứa toàn bộ các lớp Unit Tests (JUnit 5)
│
├── pom.xml                                # Tệp cấu hình Maven dependencies
└── README.md                              # Tài liệu hướng dẫn sử dụng này
```
