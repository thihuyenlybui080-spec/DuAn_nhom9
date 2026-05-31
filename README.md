# DuAn_nhom9
# AuctionSystem — Hệ thống Đấu giá Trực tuyến

Ứng dụng desktop đấu giá thời gian thực xây dựng bằng **JavaFX 21** và **SQLite**.
Hỗ trợ ba vai trò người dùng (Admin, Seller, Bidder), đặt giá thủ công, tự động đặt giá (Auto-bid),
chống bắn tỉa khi đặt giá (anti-snipe extension), và quản lý toàn bộ vòng đời phiên đấu giá từ OPEN đến PAID(OPEN-RUNNING-FINNISHED-PAID/CANCELLED).

---

## Mô tả bài toán

Hệ thống mô phỏng một sàn đấu giá trực tuyến trong đó:

- **Seller** đăng ký vật phẩm (Art, Electronics, Vehicle, Other) và mở phiên đấu giá với giá khởi điểm và khoảng thời gian tự chọn.
- **Bidder** xem các phiên đang mở, đặt giá thủ công hoặc bật chế độ tự động (Auto-bid với giá trần và bước tăng), và thanh toán sau khi thắng.
- **Admin** giám sát toàn bộ phiên, quản lý trạng thái người dùng (Active / Banned / Deleted), và có quyền hủy phiên vi phạm.
- Mọi hành động quan trọng (tạo phiên, đặt giá, kết thúc, thanh toán) đều được ghi nhận vào SQLite và không được phép sửa đổi để đảm bảo tính bền vững.

---

## Công nghệ sử dụng

| Thành phần | Công nghệ                                                        |
|---|------------------------------------------------------------------|
| Ngôn ngữ | Java 25                                                          |
| Giao diện | JavaFX 21 (FXML + CSS inline)                                    |
| Build tool | Maven 3.x (kèm Maven Wrapper `mvnw`)                             |
| Database | SQLite 3.46.0                                                    |
| JDBC Driver | SQLite JBDC Driver (khai báo trong `pom.xml`)                    |
| UI extras | BootstrapFX (styling bổ sung)                                    |


---

## Yêu cầu môi trường

- **JDK 21** trở lên (khuyến nghị Eclipse Temurin hoặc Oracle JDK 21)
- **SQLite 3.46.0** đang chạy ở localhost (hoặc máy chủ riêng)
- Maven không bắt buộc phải cài riêng vì project đã đi kèm `mvnw` / `mvnw.cmd`

---

## Cài đặt database

1. Mở MySQL client (Workbench, DBeaver, hoặc CLI).
2. Chạy toàn bộ script khởi tạo:

```sql
SOURCE path/to/loginregister.sql;
```

Script tạo database `loginregister` và 5 bảng: `users`, `items`, `auctions`, `bids`, `bid_transactions`.

3. *(Tùy chọn)* Chỉnh sửa thông tin kết nối trong `config.properties` trước khi build:

```properties
db.host=localhost
db.port=3306
db.name=loginregister
db.user=root
db.pass=root
```

---

## Cấu trúc thư mục

```
AuctionSystem_final/
├── src/main/java/org/example/loginregister/
│   ├── Launcher.java                   ← Điểm khởi động
│   ├── HelloApplication.java           ← JavaFX Application
│   ├── module-info.java
│   │
│   ├── client/
│   │   ├── controller/
│   │   │   ├── MainController.java          ← Màn hình chào
│   │   │   ├── LoginController.java         ← Đăng nhập
│   │   │   ├── RegisterController.java      ← Đăng ký
│   │   │   ├── AdminDashboardController.java
│   │   │   ├── SellerDashboardController.java
│   │   │   ├── BidderDashboardController.java
│   │   │   ├── BiddingController.java       ← Phòng đấu giá real-time
│   │   │   └── AuctionDetailController.java
│   │   ├── service/
│   │   │   └── SceneManager.java            ← Chuyển màn hình
│   │   └── util/
│   │       └── ImageLoader.java
│   │
│   ├── server/
│   │   ├── database/
│   │   │   ├── DatabaseConfig.java          ← Kết nối MySQL
│   │   │   └── AuctionDAO.java              ← Toàn bộ SQL queries
│   │   ├── model/entity/
│   │   │   ├── Entity.java                  ← Base class (ID tự sinh)
│   │   │   ├── Auction.java                 ← Logic đấu giá (thread-safe)
│   │   │   ├── AuctionStatus.java           ← Enum: OPEN→RUNNING→FINISHED→PAID
│   │   │   ├── AuctionResult.java
│   │   │   ├── BidTransaction.java
│   │   │   ├── user/
│   │   │   │   ├── User.java  ·  Admin.java  ·  Seller.java  ·  Bidder.java
│   │   │   │   ├── UserStatus.java           ← ACTIVE / BANNED / DELETED
│   │   │   │   └── UserStatusRecord.java
│   │   │   ├── item/
│   │   │   │   ├── Item.java  ·  Art.java  ·  Electronics.java  ·  Vehicle.java
│   │   │   └── auto_bidding/
│   │   │       ├── AutoBidConfig.java        ← Giá trần + bước tăng
│   │   │       └── AutoBidAgent.java         ← Observer tự động đặt giá
│   │   ├── model/factory/
│   │   │   ├── ItemFactory.java  ·  ArtFactory.java
│   │   │   ├── ElectronicsFactory.java  ·  VehicleFactory.java
│   │   └── util/
│   │       ├── AuctionManager.java           ← Singleton, điều phối phiên
│   │       └── AuctionHistoryManager.java    ← Lưu lịch sử kết thúc
│   │
│   └── common/
│       ├── observer/
│       │   ├── Observer.java  ·  Subject.java
│       └── exception/
│           ├── AuctionClosedException.java
│           ├── AuthenticationException.java
│           └── InvalidBidException.java
│
├── src/main/resources/org/example/loginregister/
│   ├── *.fxml                          ← Layout giao diện
│   ├── config.properties               ← Cấu hình database
│   └── *.png / *.jpg                   ← Tài nguyên hình ảnh
│
├── loginregister.sql                   ← Script tạo database
├── loginregister_sqlserver.sql         ← Phiên bản SQL Server (tham khảo)
├── pom.xml
└── mvnw / mvnw.cmd
```

---

## Vị trí file JAR

Sau khi build (`mvn package`), file JAR xuất hiện tại:

```
AuctionSystem_final/target/AuctionSystem_final-1.0-SNAPSHOT.jar
```

Đặt file `config.properties` **cùng thư mục** với JAR để ghi đè cấu hình database mà không cần build lại:

```
thư_mục_chạy/
├── AuctionSystem_final-1.0-SNAPSHOT.jar
└── config.properties          ← chỉnh db.host, db.user, db.pass tại đây
```

---

## Hướng dẫn chạy

### Bước 1 — Khởi động MySQL

Đảm bảo MySQL Server đang chạy và đã import `loginregister.sql`.

### Bước 2 — Build project

```bash
# Linux / macOS
./mvnw clean package -DskipTests

# Windows
mvnw.cmd clean package -DskipTests
```

Hoặc mở bằng **IntelliJ IDEA** → Import Maven project → Run `Launcher`.

### Bước 3 — Chạy ứng dụng

**Cách A — Qua IDE (khuyến nghị khi phát triển):**

Mở IntelliJ IDEA, đặt `Launcher` làm main class, nhấn Run.

**Cách B — Qua JAR (sau khi build):**

```bash
java --module-path <đường_dẫn_javafx_sdk>/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar target/AuctionSystem_final-1.0-SNAPSHOT.jar
```

> **Lưu ý:** Project đóng gói dưới dạng ứng dụng đơn, không tách Server/Client riêng.
> Toàn bộ logic (UI + business logic + DB) chạy trong cùng một JVM.
> Nhiều người dùng có thể chạy nhiều instance, tất cả kết nối chung một MySQL.

### Thứ tự sử dụng gợi ý

1. Chạy ứng dụng → màn hình chào xuất hiện.
2. Nhấn **Register** → tạo tài khoản Seller hoặc Bidder.
3. Seller đăng nhập → tạo Item → mở phiên đấu giá.
4. Bidder đăng nhập → vào phòng đấu giá → đặt giá / bật Auto-bid.
5. Sau khi phiên kết thúc → Bidder thắng vào tab "Đã thắng" → Thanh toán.
6. Admin (vào qua nút Admin trên màn hình chào) → quản lý user và phiên.

---

## Danh sách chức năng đã hoàn thành

### Xác thực & Người dùng
- [x] Đăng ký tài khoản với vai trò Bidder hoặc Seller
- [x] Đăng nhập kiểm tra với database
- [x] Validate form: độ dài mật khẩu, số điện thoại, xác nhận mật khẩu
- [x] Admin quản lý trạng thái user (Active / Banned / Deleted)
- [x] Khi ban Seller: tự động hủy tất cả phiên đang chạy của họ
- [x] Khi ban Bidder: tự động hủy tất cả bid của họ và cập nhật lại người dẫn đầu

### Vật phẩm & Phiên đấu giá
- [x] Seller thêm item theo 3 loại: Art, Electronics, Vehicle (Factory Method Pattern)
- [x] Seller xóa item khi phiên chưa bắt đầu
- [x] Mở phiên đấu giá với giá khởi điểm, thời gian bắt đầu và kết thúc tùy chọn
- [x] Lên lịch mở phiên tự động (`ScheduledExecutorService`)
- [x] Phiên tự động kết thúc đúng giờ
- [x] Anti-snipe: gia hạn thêm 60 giây nếu có bid trong 30 giây cuối
- [x] Admin hủy phiên vi phạm

### Đặt giá
- [x] Đặt giá thủ công (kiểm tra phải cao hơn giá hiện tại)
- [x] Auto-bid: tự động tăng giá theo bước khi bị vượt, dừng khi đạt giá trần
- [x] Thread-safe với `ReentrantLock(fair)` — tránh race condition khi nhiều bid đồng thời
- [x] Observer Pattern: UI và AutoBidAgent nhận thông báo real-time khi có bid mới
- [x] Hiển thị đếm ngược, danh sách lịch sử bid, người dẫn đầu hiện tại

### Kết quả & Thanh toán
- [x] Lưu `AuctionResult` sau mỗi phiên kết thúc
- [x] Bidder xem danh sách phiên đã thắng
- [x] Giả lập thanh toán: cập nhật trạng thái FINISHED → PAID
- [x] Deadline thanh toán 24 giờ: quá hạn tự động chuyển sang CANCELED

### Đồng bộ Database
- [x] Toàn bộ hành động (tạo item, tạo phiên, đặt giá, kết thúc, thanh toán) ghi vào MySQL
- [x] Đọc dữ liệu từ DB khi khởi động lại (không mất dữ liệu)
- [x] Cấu hình kết nối linh hoạt qua `config.properties`

---

## Báo cáo & Demo

| Tài nguyên | Đường dẫn |
|---|---|
| 📄 Báo cáo PDF | *(đặt link tại đây)* |
| 🎥 Video demo | *(đặt link tại đây)* |

---

## Design Patterns sử dụng

| Pattern | Vị trí áp dụng |
|---|---|
| Singleton | `AuctionManager`, `AuctionHistoryManager` |
| Observer | `Auction` (Subject) ↔ `AutoBidAgent`, `BiddingController` (Observer) |
| Factory Method | `ItemFactory` → `ArtFactory`, `ElectronicsFactory`, `VehicleFactory` |
| Template Method | `User.onStatusChanged()` — hook cho Seller và Bidder override |

---

## Tác giả

| Họ tên | MSSV |
|---|---|
| Bùi Thị Huyền Ly | 25021861 |
| Trần Thị Thu Thủy | 25022024 |
| Trần Lâm Dương | 25021702 |
| Bùi Đồng Nhất | 25021922 |


