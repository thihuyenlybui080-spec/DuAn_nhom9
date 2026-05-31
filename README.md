# 🏷️ Auction System — Hệ Thống Đấu Giá Trực Tuyến

Hệ thống đấu giá trực tuyến theo mô hình **Client-Server**, cho phép nhiều người dùng đồng thời tham gia các phiên đấu giá theo thời gian thực. Server xử lý toàn bộ nghiệp vụ và cơ sở dữ liệu; Client cung cấp giao diện đồ họa JavaFX để người dùng tương tác.

---

## 1.  Mô tả bài toán & Phạm vi hệ thống
 
### Bài toán đặt ra
 
Trong các hệ thống đấu giá trực tuyến, việc xử lý hàng ngàn yêu cầu đặt giá (Bid) cùng một thời điểm thường dẫn đến các vấn đề nghiêm trọng về **tranh chấp dữ liệu (Race Condition)**. Đặc biệt, hiện tượng **"bắn tỉa giá" (Sniping)** — người tham gia đợi đến giây cuối cùng để đặt giá khiến người khác không kịp phản hồi — làm giảm tính minh bạch và công bằng của phiên đấu giá.
 
### Phạm vi giải quyết
 
Hệ thống được xây dựng để giải quyết trọn vẹn các thách thức trên thông qua **kiến trúc phân tán phân lớp**, gồm hai thành phần độc lập:
 
**Phía Server (Backend)** — đóng vai trò trung tâm điều phối:
- Lắng nghe đa kết nối đồng thời qua TCP Socket, mỗi client được phục vụ trên một **Virtual Thread** riêng (Java 21).
- Quản lý toàn bộ vòng đời phiên đấu giá: tạo, mở, kết thúc, huỷ.
- Đồng bộ trạng thái và **push thông báo real-time** đến tất cả client đang xem phiên khi có biến động giá.
- Tự động kích hoạt **bộ đếm ngược chống bắn tỉa (Anti-Snipe)**: gia hạn thêm 60 giây khi có bid trong 30 giây cuối, giúp những người tham gia khác không bị bất ngờ.
- Xử lý chức năng **tự động đặt giá (Auto-Bid)** theo thứ tự ưu tiên FIFO.
- Tích hợp cổng **thanh toán Stripe** và lưu trữ dữ liệu bền vững qua SQLite.
**Phía Client (Frontend)** — cung cấp giao diện người dùng chuyên nghiệp:
- Giao diện đồ hoạ GUI xây dựng bằng **JavaFX**, hỗ trợ 3 vai trò: Admin, Seller, Bidder.
- Gửi yêu cầu dưới dạng gói tin đối tượng (Java Object Serialization) và nhận phản hồi đồng bộ.
- Lắng nghe và hiển thị **cập nhật giá tức thời** từ server mà không cần polling.
### Các đối tượng trong hệ thống

| Đối tượng | Chức năng chính |
|-----------|----------------|
| **Admin** | Quản lý người dùng, khoá/mở tài khoản, xem lịch sử toàn hệ thống |
| **Seller** | Đăng vật phẩm (Art / Electronics / Vehicle / Other), tạo phiên đấu giá, theo dõi kết quả |
| **Bidder** | Xem danh sách phiên, đặt giá thủ công, cấu hình auto-bid, thanh toán khi thắng |

Giao tiếp qua **TCP Socket (port 8080)** với giao thức Request/Response + push Notification dùng Java Object Serialization.

---

## 2. Công nghệ sử dụng

### Server
| Thành phần | Chi tiết |
|-----------|---------|
| Ngôn ngữ | Java 21 (Virtual Threads) |
| Build tool | Apache Maven |
| Database | SQLite 3.46 (`sqlite-jdbc`) |
| Connection pool | HikariCP |
| Thanh toán | Stripe Java SDK 26.3.0 |
| Config | SnakeYAML |
| Test | JUnit Jupiter 5.10.2 + Mockito |

### Client
| Thành phần | Chi tiết |
|-----------|---------|
| Ngôn ngữ | Java 21 |
| UI | JavaFX 25 + FXML |
| Thư viện UI bổ sung | ControlsFX 11.2.1, FormsFX, BootstrapFX |
| Config | `application.yaml` (SnakeYAML) |
| Build tool | Apache Maven |

---

## 3. Yêu cầu môi trường

- **Java 21** trở lên (bắt buộc — Server dùng Virtual Thread, Client dùng JavaFX 25)
- **Maven 3.8+** (nếu build từ source)
- Không cần cài database riêng — SQLite là file nhúng, tự khởi tạo khi chạy lần đầu
- Không cần cài JavaFX riêng — đã đóng gói vào JAR của Client

Kiểm tra phiên bản Java:
```bash
java -version
# output cần: openjdk 21 ...
```

---

## 4. Cấu trúc thư mục

```
AuctionServer/
├── src/
│   ├── main/
│   │   ├── java/vn/edu/vnu/auction/
│   │   │   ├── AuctionServer.java          # Entry point server
│   │   │   ├── ClientHandler.java          # Xử lý 1 client / thread
│   │   │   ├── ClientRegistry.java         # Quản lý socket stream của clients
│   │   │   ├── common/
│   │   │   │   ├── network/                # Request, Response, NotificationMessage
│   │   │   │   ├── observer/               # Interface Observer, Subject
│   │   │   │   └── exception/              # 4 custom exceptions
│   │   │   ├── model/entity/               # Entity, User, Bidder, Seller, Admin
│   │   │   │   ├── item/                   # Item, Art, Electronics, Vehicle, Other
│   │   │   │   ├── factory/                # ItemFactory + 4 concrete factories
│   │   │   │   └── auto_bidding/           # AutoBid, AutoBidConfig
│   │   │   ├── service/                    # AuctionService, BidService, AutobidService
│   │   │   │   │                             UserService, ItemService, PaymentService
│   │   │   ├── dao/                        # AuctionDAO, BidDAO, UserDAO, ItemDAO
│   │   │   │   │                             AutoBidDAO, LoginHistoryDAO
│   │   │   ├── database/                   # DatabaseConfig (HikariCP + SQLite)
│   │   │   └── util/                       # AuctionManager, AuctionHistoryManager
│   │   └── resources/
│   │       ├── config.properties           # Cấu hình port, DB name, Stripe key
│   │       └── auction_system.sql          # Schema khởi tạo DB
│   └── test/                               # JUnit tests
├── auction_system.db                       # File DB SQLite (tự tạo khi chạy lần đầu)
├── pom.xml
└── target/
    └── AuctionServer-1.0.jar               # ← FAT JAR chạy được

AuctionClient/
├── src/
│   ├── main/
│   │   ├── java/vn/edu/vnu/auction/
│   │   │   ├── Launcher.java               # Entry point (wrapper tránh lỗi module JavaFX)
│   │   │   ├── AuctionApplication.java     # JavaFX Application — khởi động UI
│   │   │   ├── controller/                 # LoginController, RegisterController
│   │   │   │   │                             MainController, BiddingController
│   │   │   │   │                             BidderDashboardController, SellerDashboardController
│   │   │   │   │                             AdminDashboardController, AuctionDetailController
│   │   │   │   │                             PaymentGatewayController, ToastNotification
│   │   │   ├── service/                    # ConnectionManager, MessageRouter
│   │   │   │   │                             AuctionClientService, NotificationListener
│   │   │   │   └──                           SceneManager
│   │   │   ├── model/                      # Shared model (mirror từ Server)
│   │   │   │   ├── entity/                 # Auction, Item, User, BidTransaction...
│   │   │   │   └── factory/                # ItemFactory + 4 factories
│   │   │   └── util/                       # ImageLoader
│   │   └── resources/
│   │       ├── application.yaml            # Cấu hình IP/port server
│   │       └── vn/edu/vnu/auctionclient/   # File FXML giao diện
├── pom.xml
└── target/
    └── AuctionClient-1.0.jar               # ← FAT JAR chạy được
```

---

## 5. Vị trí file JAR

Sau khi build (hoặc giải nén từ bản nộp), các file JAR nằm tại:

```
AuctionServer/target/AuctionServer-1.0.jar
AuctionClient/target/AuctionClient-1.0.jar
```

> **Lưu ý:** Đây là **fat JAR** (uber JAR) — đã đóng gói toàn bộ dependency bên trong.  
> File `original-AuctionServer-1.0.jar` / `original-AuctionClient-1.0.jar` là JAR gốc không có dependency, **không chạy được độc lập**.

---

## 6. Hướng dẫn chạy

> ⚠️ **Phải chạy Server trước, sau đó mới chạy Client.**

### Bước 1 — Cấu hình Server (tuỳ chọn)

Mặc định Server chạy trên port `8080`, DB file là `auction_system.db`.  
Để thay đổi, tạo file `config.properties` **cùng thư mục với JAR**:

```properties
db.name=auction_system.db
server.port=8080
stripe.secret.key=sk_test_xxxx
```

Nếu không có file này, Server tự dùng giá trị mặc định.

---

### Bước 2 — Chạy Server

```bash
cd AuctionServer
java -jar target\AuctionServer-1.0.jar
```

Khi thấy log:
```
Server started on port 8080
```
Server đã sẵn sàng. Database sẽ tự khởi tạo lần đầu với 4 tài khoản Admin mặc định.

---

### Bước 3 — Cấu hình Client

Mặc định Client kết nối đến `localhost:8080`.  
Nếu Server chạy trên máy khác, tạo file `application.yaml` **cùng thư mục với JAR** của Client:

```yaml
server:
  ip: 192.168.x.x   # IP của máy chạy Server
  port: 8080
```

---

### Bước 4 — Chạy Client

```bash
cd AuctionClient
java -jar target\AuctionClient-1.0.jar
```

Cửa sổ đăng nhập JavaFX sẽ hiện ra.

---

### Tài khoản Admin mặc định

| Username | Password |
|----------|----------|
| `huyenly` | `123456789` |
| `thuthuy` | `123456789` |
| `lamduong` | `123456789` |
| `dongnhat` | `123456789` |

Có thể đăng ký tài khoản Bidder hoặc Seller mới trực tiếp trên giao diện.

---

### Build từ source (nếu cần)

```bash
# Build Server
cd AuctionServer
mvn clean package -DskipTests

# Build Client
cd ../AuctionClient
mvn clean package -DskipTests
```

---

## 7. Danh sách chức năng đã hoàn thành

### 🔐 Xác thực & Tài khoản
-  Đăng ký tài khoản (Bidder / Seller)
-  Đăng nhập với xác thực username + password
-  Ghi lịch sử đăng nhập (thành công / thất bại)
-  Khoá / mở khoá tài khoản (Admin)

### 🏷️ Quản lý vật phẩm & Phiên đấu giá
-  Seller đăng vật phẩm (4 loại: Art, Electronics, Vehicle, Other)
-  Tạo phiên đấu giá với thời gian bắt đầu và kết thúc tuỳ chỉnh
-  Phiên tự động mở (`OPEN → RUNNING`) theo lịch đã đặt
-  Phiên tự động kết thúc (`RUNNING → FINISHED`) khi hết giờ
-  Admin xem toàn bộ phiên, huỷ phiên tuỳ ý

### 💰 Đặt giá
-  Đặt giá thủ công — kiểm tra amount > current_price
-  Transaction SERIALIZABLE — chống race condition nhiều bidder cùng lúc
-  Cập nhật giá real-time cho tất cả client đang xem (push notification)
-  Xem lịch sử các lần đặt giá trong phiên

### 🤖 Auto-Bid
-  Bidder cấu hình max_bid và increment cho từng phiên
-  Hệ thống tự động đặt giá theo thứ tự FIFO khi giá bị vượt
-  Tắt auto-bid khi vượt max_bid hoặc bidder bị khoá
-  Persist cấu hình auto-bid vào DB để khôi phục khi restart

### ⏱️ Anti-Snipe
-  Tự động gia hạn thêm 60 giây nếu có bid trong 30 giây cuối
-  Notify tất cả client về việc gia hạn

### 💳 Thanh toán
-  Tích hợp Stripe Payment Gateway
-  Deadline thanh toán 24h sau khi phiên kết thúc
-  Phiên chuyển `PAID` sau khi thanh toán thành công
-  Phiên chuyển `CANCEL` nếu không thanh toán trong 24h

### 📊 Dashboard
-  Bidder: xem phiên đang tham gia, lịch sử thắng, cấu hình auto-bid
-  Seller: xem phiên do mình tạo, trạng thái và kết quả
-  Admin: quản lý toàn bộ user, xem login history, thống kê hệ thống

### 🔧 Kỹ thuật
-  Kiến trúc phân tầng: Network → Service → DAO → Database
-  Design Pattern: Singleton, Observer, Factory Method
-  HikariCP connection pool + SQLite WAL mode cho concurrent access
-  Virtual Thread (Java 21) — mỗi client 1 thread nhẹ
-  Graceful shutdown với WAL checkpoint
-  Unit test với JUnit 5 + Mockito

---

## 8. Tài liệu & Demo

| Loại | Link |
|------|------|
| 📄 Báo cáo PDF | https://drive.google.com/file/d/1f6l3Tn1UQ0D2n3OwKW1GD3DTw9lGkRQK/view?usp=sharing |
| 🎬 Video demo | https://drive.google.com/file/d/1BdCKOhwVJGh3uZmlIO-nS9eIDV27HsxF/view |

---

## 9. Thành viên nhóm

| Họ tên | MSSV |
|---|---|
| Bùi Thị Huyền Ly | 25021861 |
| Trần Thị Thu Thủy | 25022024 |
| Trần Lâm Dương | 25021702 |
| Bùi Đồng Nhất | 25021922 |


