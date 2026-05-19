package org.example.loginregister.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * ================================================================
 *  CẤU HÌNH KẾT NỐI DATABASE  –  Chỉ sửa file này khi đổi máy
 * ================================================================
 *
 *  HƯỚNG DẪN THIẾT LẬP (làm 1 lần trên máy cài MySQL):
 *
 *  BƯỚC 1 – Tìm IP máy bạn:
 *    Mở CMD → gõ "ipconfig" → lấy IPv4 Address (vd: 192.168.1.5)
 *    Điền vào DB_HOST bên dưới.
 *
 *  BƯỚC 2 – Cho phép kết nối từ xa trong MySQL Workbench:
 *    CREATE USER 'root'@'%' IDENTIFIED BY 'root';
 *    GRANT ALL PRIVILEGES ON loginregister.* TO 'root'@'%';
 *    FLUSH PRIVILEGES;
 *
 *  BƯỚC 3 – Mở port 3306 trên Windows Firewall:
 *    Control Panel → Windows Defender Firewall → Advanced Settings
 *    → Inbound Rules → New Rule → Port → TCP 3306 → Allow
 *
 *  Các máy khác chỉ cần cài Java, KHÔNG cần cài MySQL.
 * ================================================================
 */
public class DatabaseConfig {

    // ★ SỬA DÒNG NÀY: thay bằng IP máy bạn (xem bằng lệnh ipconfig)
    private static final String DB_HOST = "192.168.56.1";

    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "loginregister";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";

    private static final String DB_URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh";

    /**
     * Lấy một Connection tới MySQL server.
     * Dùng trong try-with-resources để tự đóng connection.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    /** Chuỗi mô tả server — hiển thị khi báo lỗi kết nối. */
    public static String getServerInfo() {
        return DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    }
}
