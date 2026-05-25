package vn.edu.vnu.auction.dao;

import vn.edu.vnu.auction.database.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class LoginHistoryDAO {
    private static final Logger logger = LoggerFactory.getLogger(LoginHistoryDAO.class);

    /** Lưu lịch sử đăng nhập. userId = -1 nếu đăng nhập thất bại. */
    public static void saveLoginHistory(int userId, String username, String status) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "INSERT INTO login_history (user_id, username, status) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (userId == -1) stmt.setNull(1, Types.INTEGER);
            else stmt.setInt(1, userId);
            stmt.setString(2, username);
            stmt.setString(3, status);
            stmt.executeUpdate();
            logger.info("[LoginHistoryDAO] Saved: {} - {}", username, status);
        } catch (Exception e) {
            logger.error("[LoginHistoryDAO] Failed to save login history: {}", e.getMessage());
        }
    }

    /** Lấy lịch sử đăng nhập. */
    public static ResultSet getLoginHistory() {
        try {
            Connection conn = DatabaseConfig.getConnection();
            String sql = "SELECT user_id, username, status, login_time FROM login_history ORDER BY login_time DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            return stmt.executeQuery();
        } catch (Exception e) {
            logger.error("[LoginHistoryDAO] Failed to get login history: {}", e.getMessage());
            return null;
        }
    }
}
