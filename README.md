# DuAn_nhom9
# Hệ Thống Quản Lý Đấu Giá Tự Động (Automated Auction Management System)

Hệ thống quản lý đấu giá tự động theo mô hình **Client - Server** thời gian thực, tích hợp giao diện đồ họa trực quan **JavaFX**, cơ chế kiểm soát đồng thời mạnh mẽ và logic chống bắn tỉa khi đặt giá (Anti-sniping). Dự án được cấu trúc và quản lý bằng **Maven**, tuân thủ nghiêm ngặt các nguyên lý thiết kế phần mềm hướng đối tượng (OOP) và tiêu chuẩn mã nguồn Google Java Style.

---

## 1. Mô tả bài toán và Phạm vi hệ thống

* **Bài toán đặt ra:** Trong các hệ thống đấu giá trực tuyến, việc xử lý hàng ngàn yêu cầu đặt giá (Bid) cùng một thời điểm thường dẫn đến các vấn đề nghiêm trọng về tranh chấp dữ liệu (Race Condition). Đặc biệt, hiện tượng "bắn tỉa giá" (Sniping) — người tham gia đợi đến giây cuối cùng để đặt giá khiến người khác không kịp phản hồi — làm giảm tính minh bạch của phiên đấu giá.
* **Phạm vi giải quyết:** Hệ thống được xây dựng để giải quyết trọn vẹn các thách thức trên thông qua kiến trúc phân tán phân lớp:
    * **Phía Server (Backend):** Đóng vai trò là trung tâm điều phối, lắng nghe đa kết nối qua Socket. Server quản lý vòng đời phiên đấu giá, đồng bộ trạng thái, tự động kích hoạt bộ đếm ngược chống bắn tỉa giúp những người tham gia khác không bị bối rối, xử lý chức năng tự động đặt giá (Auto-bid), tích hợp thanh toán và lưu trữ dữ liệu.
    * **Phía Client (Frontend):** Cung cấp giao diện người dùng GUI (JavaFX) chuyên nghiệp. Client tiếp nhận tương tác, gửi yêu cầu dưới dạng gói tin đối tượng và cập nhật tức thời các biến động giá theo thời gian thực (Real-time).

---

## 2. Kiến trúc Công nghệ & Yêu cầu Môi trường

* **Ngôn ngữ lập trình:** Java (Java SE 8+ / Khuyến nghị JDK ).
* **Giao diện người dùng (GUI):** JavaFX (Sử dụng kiến trúc MVC với các tệp `.fxml`).
* **Quản lý dự án & Build:** Apache Maven.
* **Cơ sở dữ liệu & Tối ưu hóa:** * **SQLite ** .
    * **HikariCP:** Bộ quản lý bể chứa kết nối (Connection Pool) hiệu năng cao, duy trì `MaximumPoolSize = 20` giúp tối ưu hóa tài nguyên mạng.
* **Logging:** SLF4J kết hợp Logback ghi vết hệ thống chuẩn công nghiệp.
* **Kiểm thử tự động (Testing):** JUnit 5 và Mockito bao phủ các luồng nghiệp vụ.
* **Yêu cầu cài đặt:** Máy tính đã cấu hình `JAVA_HOME`  đang hoạt động.

---

## 3. Cấu trúc Thư mục và Các Module chính

Dự án được tổ chức theo chuẩn Maven:

* `src/main/java/org/example/loginregister/client/`: Tầng điều khiển giao diện & mạng phía Client (Gồm `controller`, `service`, `util`).
* `src/main/java/org/example/loginregister/server/`: Tầng xử lý logic nghiệp vụ và lưu trữ phía Server.
    * `common/`: Các Exception tùy chỉnh và lớp cấu trúc mạng dùng chung.
    * `dao/` & `database/`: Data Access Object và cấu hình HikariCP (`DatabaseConfig`).
    * `model/`: Các thực thể Entity (`Auction`, `Item`, `BidTransaction`, `User`...).
    * `service/`: Logic lõi (`AuctionService`, `BidService`, `PaymentService`...).
    * Lớp mạng lõi: `AuctionServer` (Lắng nghe Socket), `ClientHandler` (Xử lý Request), `ClientRegistry` (Broadcast thông báo).
* `src/main/resources/`: Lưu trữ file giao diện `.fxml`, hình ảnh và cấu hình log.
* `src/test/`: Mã nguồn Unit Test và Mock testing.
* `auction_system.sql`: Kịch bản khởi tạo cấu trúc CSDL.

---

## 4. Thiết kế Giao thức Tầng Ứng dụng (Application Layer Protocol)

Hệ thống thiết kế một giao thức truyền tải hướng kết nối hoạt động trên nền tảng **TCP**, đóng gói dữ liệu qua cơ chế **Java Object Serialization** (`ObjectInputStream`/`ObjectOutputStream`). Điều này đảm bảo tính toàn vẹn tuyệt đối và thứ tự truyền nhận của gói tin.

Giao thức quy định 3 loại cấu trúc đối tượng trao đổi qua mạng:
1. **`Request` (Client ──> Server):** Gói tin yêu cầu mang `requestId` duy nhất, mã lệnh `action` (VD: `ACTION_PLACE_BID`, `ACTION_WATCH_AUCTION`) và phần thân `data`, giúp người dùng có thể tương tác trực tuyến với server .
2. **`Response` (Server ──> Client):** Gói tin phản hồi mang trạng thái (`OK`/`ERROR`), thông báo chi tiết và đối tượng kết quả trả về, đại diện cho phản ứng của server đối với yêu cầu của client .
3. **`NotificationMessage` (Server ──» Tất cả Client):** Gói tin phát sóng (Broadcast) gửi từ `ClientRegistry` để báo động khẩn (VD: `TYPE_BID_UPDATED`, `TYPE_TIME_EXTENDED`) giúp Client đồng bộ giao diện tức thì, thông báo những thay đổi về cuộc đấu giá cho tất cả những người tham gia tại thời điểm đó .

---

## 5. Vị trí các tệp thực thi (.jar)

Nhóm sử dụng `maven-shade-plugin` để đóng gói toàn bộ thư viện (bao gồm JavaFX và JDBC driver) vào thành file executable fat JAR.
* **File chạy Server:** `target/server-fat.jar`
* **File chạy Client:** `target/client-fat.jar`

---

## 6. Hướng dẫn chạy Server/Client theo thứ tự cụ thể

**Bước 1: Đóng gói(Package) **

**Bước 2: Khởi động Server**
Mở Terminal tại thư mục chứa file jar của Server, chạy lệnh kèm tham số là số `PORT` muốn sử dụng (Server sử dụng *Virtual Threads* để xử lý đa luồng):
```bash
java -jar target/server-fat.jar 8080
**Bước 3: Khởi động Client**
Mở một Terminal/Command Prompt khác tại thư mục chứa file jar của Client và chạy lệnh kèm 2 tham số là `Server IP` và `PORT` (phải khớp với Server):
```bash
java -jar target/client-fat.jar 127.0.0.1 8080
```
## 7. Danh sách chức năng đã hoàn thành
[x] Kiến trúc Mạng: Phân tán Client-Server qua TCP Socket. Giao tiếp an toàn bằng Object Serialization. Broadcast đồng bộ trạng thái mượt mà bằng ClientRegistry.

[x] Xử lý Đồng thời (Concurrency): Sử dụng khóa ReentrantLock độc lập cho từng phiên đấu giá, triệt tiêu lỗi Race Condition khi nhiều Bidder trả giá tại cùng một mili-giây.

[x] Logic Đấu giá: Khởi tạo, tham gia, và rời phiên đấu giá.

[x] Anti-sniping (Chống bắn tỉa): Hệ thống tự động tính toán, đếm ngược và phát thông báo "Going once", "Going twice", "Sold", hoặc tự động gia hạn thời gian nếu có lượt đặt giá mới ở phút chót.

[x] Auto-bidding (Đặt giá tự động): Người mua thiết lập giá trần và bước nhảy, hệ thống (AutoBidDAO) tự động thay mặt người mua nâng giá khi có đối thủ.

[x] Dịch vụ tích hợp: Lưu vết lịch sử đấu giá (AuctionHistoryManager) và quyết toán thanh toán phiên (PaymentService).

[x] Giao diện & Phân quyền: Phân quyền rõ ràng Admin, Seller, Bidder qua UI JavaFX thân thiện.
