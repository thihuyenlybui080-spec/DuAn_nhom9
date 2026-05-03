-- ============================================
--  Database: LoginRegister
--  Dành cho ứng dụng JavaFX Login/Register
-- ============================================

CREATE DATABASE IF NOT EXISTS loginregister
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE loginregister;

-- ============================================
--  Bảng users
-- ============================================
CREATE TABLE IF NOT EXISTS users (
                                     id          INT          NOT NULL AUTO_INCREMENT,
                                     firstname   VARCHAR(100) NOT NULL,
    lastname    VARCHAR(100) NOT NULL,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,   -- nên lưu dạng hash (BCrypt...)
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
--  Dữ liệu mẫu (tuỳ chọn)
-- ============================================
-- Mật khẩu mẫu dưới đây là plain-text để test.
-- Trong thực tế hãy hash trước khi INSERT.


-- ============================================
--  Truy vấn dùng trong LoginController
-- ============================================
-- validateLogin():
--   SELECT id FROM users
--   WHERE username = ? AND password = ?;

-- ============================================
--  Truy vấn dùng trong RegisterController
-- ============================================
-- registerUser():
--   INSERT INTO users (firstname, lastname, username, password)
--   VALUES (?, ?, ?, ?);

-- Kiểm tra username đã tồn tại chưa:
--   SELECT COUNT(*) FROM users WHERE username = ?;
SELECT * FROM users;
