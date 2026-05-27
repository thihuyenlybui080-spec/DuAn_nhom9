-- ============================================================
--  Database: AuctionSystem (SQLite version)
--  Dành cho ứng dụng JavaFX Login/Register + Auction
-- ============================================================

-- ============================================================
--  Bảng users (Bidder + Seller + Admin)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id          INTEGER      NOT NULL PRIMARY KEY AUTOINCREMENT,
    username    TEXT(50)     NOT NULL UNIQUE,
    password    TEXT(255)    NOT NULL,
    email       TEXT(150)    NOT NULL,
    full_name   TEXT(200)    NOT NULL,
    gender      TEXT(10)     NOT NULL DEFAULT 'Other',
    phone       TEXT(11)     NOT NULL,
    role        TEXT         NOT NULL DEFAULT 'BIDDER' CHECK (role IN ('BIDDER','SELLER','ADMIN')),
    status      TEXT         NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','BANNED')),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- ============================================================
--  Bảng login_history (lịch sử đăng nhập)
-- ============================================================
CREATE TABLE IF NOT EXISTS login_history (
    id          INTEGER      NOT NULL PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER,          -- ← đổi thành NULL
    username    TEXT(50)     NOT NULL,
    login_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address  TEXT(50),
    status      TEXT         NOT NULL CHECK (status IN ('SUCCESS', 'FAILED')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
    );

-- ============================================================
--  Bảng items (Art, Electronics, Vehicle)
--  Dùng cột item_type thay vì 3 bảng riêng
-- ============================================================
CREATE TABLE IF NOT EXISTS items (
    id              INTEGER      NOT NULL PRIMARY KEY AUTOINCREMENT,
    item_name       TEXT(200)    NOT NULL,
    description     TEXT,
    item_type       TEXT         NOT NULL CHECK (item_type IN ('ART','ELECTRONICS','VEHICLE','OTHER')),
    starting_price  REAL         NOT NULL,
    current_price   REAL         NOT NULL,
    start_time      DATETIME     NOT NULL,
    end_time        DATETIME     NOT NULL,
    created_by      INTEGER      NOT NULL,
    image_path      TEXT(500),
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT
    );

-- ============================================================
--  Bảng auctions
--  status khớp với hằng số trong AuctionStatus.java
--  [SỬA] Thêm 'PAID' để khớp với AuctionStatus.PAID trong Java
-- ============================================================
CREATE TABLE IF NOT EXISTS auctions (
    id                  INTEGER      NOT NULL PRIMARY KEY AUTOINCREMENT,
    item_id             INTEGER      NOT NULL,
    status              TEXT         NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','RUNNING','FINISHED','CANCELED','PAID')),
    current_price       REAL         NOT NULL,
    highest_bidder_id   INTEGER,
    duration_seconds    INTEGER      NOT NULL,
    end_time            DATETIME     NOT NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE RESTRICT,
    FOREIGN KEY (highest_bidder_id) REFERENCES users(id) ON DELETE SET NULL
    );

-- ============================================================
--  Bảng bids (mỗi lần placeBid() thành công → 1 dòng)
-- ============================================================
CREATE TABLE IF NOT EXISTS bids (
    id          INTEGER      NOT NULL PRIMARY KEY AUTOINCREMENT,
    auction_id  INTEGER      NOT NULL,
    bidder_id   INTEGER      NOT NULL,
    amount      REAL         NOT NULL,
    bid_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE RESTRICT
    );

-- ============================================================
--  Bảng bid_transactions (ghi 1 lần khi finishAuction())
-- ============================================================
CREATE TABLE IF NOT EXISTS bid_transactions (
    id               INTEGER      NOT NULL PRIMARY KEY AUTOINCREMENT,
    auction_id       INTEGER      NOT NULL,
    bidder_id        INTEGER      NOT NULL,
    item_id          INTEGER      NOT NULL,
    final_amount     REAL         NOT NULL,
    transaction_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE RESTRICT,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE RESTRICT
    );

-- ============================================================
--  Bảng auto_bids (lưu cấu hình auto-bid của bidder)
-- ============================================================
CREATE TABLE IF NOT EXISTS auto_bids (
    id              INTEGER      NOT NULL PRIMARY KEY AUTOINCREMENT,
    auction_id      INTEGER      NOT NULL,
    bidder_id       INTEGER      NOT NULL,
    max_bid         REAL         NOT NULL,
    increment       REAL         NOT NULL DEFAULT 1.0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (auction_id, bidder_id),
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- ============================================================
--  INDEX
-- ============================================================
CREATE INDEX idx_auctions_status ON auctions(status);
CREATE INDEX idx_bids_auction    ON bids(auction_id);
CREATE INDEX idx_bids_bidder     ON bids(bidder_id);
CREATE INDEX idx_items_type      ON items(item_type);

-- ============================================================
--  DỮ LIỆU MẪU — 4 Admin
-- ============================================================
INSERT INTO users (username, password, email, full_name, gender, phone, role) VALUES
                                                                                  ('huyenly',  '123456789', 'huyenly@auction.com',  'Bui Thi Huyen Ly',  'Female', '0900000001', 'ADMIN'),
                                                                                  ('thuthuy',  '123456789', 'thuthuy@auction.com',  'Tran Thi Thu Thuy', 'Female', '0900000002', 'ADMIN'),
                                                                                  ('lamduong', '123456789', 'lamduong@auction.com', 'Tran Lam Duong',    'Female', '0900000003', 'ADMIN'),
                                                                                  ('dongnhat', '123456789', 'dongnhat@auction.com', 'Bui Dong Nhat',     'Male',   '0900000004', 'ADMIN');


-- ============================================================
--  TRUY VẤN DÙNG TRONG CONTROLLER
-- ============================================================

-- [LoginController] Xác thực:
--   SELECT id, full_name, role
--   FROM users WHERE username = ? AND password = ?;

-- [RegisterController] Đăng ký:
--   INSERT INTO users (username, password, email, full_name, gender, phone, role)
--   VALUES (?, ?, ?, ?, ?, ?, ?);
--
--   Kiểm tra username trùng:
--   SELECT COUNT(*) FROM users WHERE username = ?;

-- [MainController] Danh sách auction đang mở:
--   SELECT a.id, i.item_name, i.item_type, a.status,
--          a.current_price, a.end_time, u.username AS highest_bidder
--   FROM auctions a
--   JOIN items i ON a.item_id = i.id
--   LEFT JOIN users u ON a.highest_bidder_id = u.id
--   WHERE a.status IN ('OPEN','RUNNING')
--   ORDER BY a.end_time ASC;

-- [Auction.placeBid()] Đặt giá:
--   INSERT INTO bids (auction_id, bidder_id, amount) VALUES (?, ?, ?);
--   UPDATE auctions SET current_price = ?, highest_bidder_id = ?, status = 'RUNNING'
--   WHERE id = ?;

-- [Auction.finishAuction()] Kết thúc:
--   UPDATE auctions SET status = 'FINISHED' WHERE id = ?;
--   INSERT INTO bid_transactions (auction_id, bidder_id, item_id, final_amount)
--   VALUES (?, ?, ?, ?);

-- [Admin.cancelAuction()] Huỷ:
--   UPDATE auctions SET status = 'CANCELED' WHERE id = ?;

-- [Lịch sử bidder]:
--   SELECT b.amount, b.bid_time, i.item_name, a.status
--   FROM bids b
--   JOIN auctions a ON b.auction_id = a.id
--   JOIN items i ON a.item_id = i.id
--   WHERE b.bidder_id = ?
--   ORDER BY b.bid_time DESC;

