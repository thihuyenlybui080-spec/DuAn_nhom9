-- ============================================================
--  Database: LoginRegister (AuctionSystem)
--  Dành cho ứng dụng JavaFX Login/Register + Auction
-- ============================================================

CREATE DATABASE IF NOT EXISTS loginregister
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE loginregister;

-- ============================================================
--  Bảng users (Bidder + Seller + Admin)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id          INT          NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    full_name   VARCHAR(200) NOT NULL,
    gender      VARCHAR(10)  NOT NULL DEFAULT 'Other',
    phone       VARCHAR(11)  NOT NULL,
    role        ENUM('BIDDER','SELLER','ADMIN') NOT NULL DEFAULT 'BIDDER',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  Bảng items (Art, Electronics, Vehicle)
--  Dùng cột item_type thay vì 3 bảng riêng
-- ============================================================
CREATE TABLE IF NOT EXISTS items (
    id              INT          NOT NULL AUTO_INCREMENT,
    item_name       VARCHAR(200) NOT NULL,
    description     TEXT,
    item_type       ENUM('ART','ELECTRONICS','VEHICLE') NOT NULL,
    starting_price  DOUBLE       NOT NULL,
    current_price   DOUBLE       NOT NULL,
    start_time      DATETIME     NOT NULL,
    end_time        DATETIME     NOT NULL,
    created_by      INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_item_admin FOREIGN KEY (created_by)
    REFERENCES users(id) ON DELETE RESTRICT
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  Bảng auctions
--  status khớp với hằng số trong Auction.java
-- ============================================================
CREATE TABLE IF NOT EXISTS auctions (
    id                  INT         NOT NULL AUTO_INCREMENT,
    item_id             INT         NOT NULL,
    status              ENUM('OPEN','RUNNING','FINISHED','CANCELED') NOT NULL DEFAULT 'OPEN',
    current_price       DOUBLE      NOT NULL,
    highest_bidder_id   INT         NULL,
    duration_seconds    BIGINT      NOT NULL,
    end_time_millis     BIGINT      NOT NULL,
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_auction_item   FOREIGN KEY (item_id)
    REFERENCES items(id) ON DELETE RESTRICT,
    CONSTRAINT fk_auction_bidder FOREIGN KEY (highest_bidder_id)
    REFERENCES users(id) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  Bảng bids (mỗi lần placeBid() thành công → 1 dòng)
-- ============================================================
CREATE TABLE IF NOT EXISTS bids (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    auction_id  INT         NOT NULL,
    bidder_id   INT         NOT NULL,
    amount      DOUBLE      NOT NULL,
    bid_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY (id),
    CONSTRAINT fk_bid_auction FOREIGN KEY (auction_id)
    REFERENCES auctions(id) ON DELETE CASCADE,
    CONSTRAINT fk_bid_bidder  FOREIGN KEY (bidder_id)
    REFERENCES users(id) ON DELETE RESTRICT
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  Bảng bid_transactions (ghi 1 lần khi finishAuction())
-- ============================================================
CREATE TABLE IF NOT EXISTS bid_transactions (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    auction_id       INT         NOT NULL,
    bidder_id        INT         NOT NULL,
    item_id          INT         NOT NULL,
    final_amount     DOUBLE      NOT NULL,
    transaction_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_tx_auction FOREIGN KEY (auction_id)
    REFERENCES auctions(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tx_bidder  FOREIGN KEY (bidder_id)
    REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tx_item    FOREIGN KEY (item_id)
    REFERENCES items(id) ON DELETE RESTRICT
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
                                                                                  ('lamduong', '123456789', 'lamduong@auction.com', 'Tran Lam Duong',    'Female',   '0900000003', 'ADMIN'),
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
--          a.current_price, a.end_time_millis, u.username AS highest_bidder
--   FROM auctions a
--   JOIN items i ON a.item_id = i.id
--   LEFT JOIN users u ON a.highest_bidder_id = u.id
--   WHERE a.status IN ('OPEN','RUNNING')
--   ORDER BY a.end_time_millis ASC;

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

SELECT * FROM users;
SELECT * FROM items;
SELECT * FROM auctions;
SELECT * FROM bids;
SELECT * FROM bid_transactions;