package org.example.loginregister.server.dao;

import org.example.loginregister.client.service.AuctionClientService;
import org.example.loginregister.server.database.DatabaseConfig;

import java.sql.*;

public class LoginHistoryDAO {

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
            System.out.println("[LoginHistoryDAO] Saved: " + username + " - " + status);
        } catch (Exception e) {
            System.err.println("[LoginHistoryDAO] Failed to save login history: " + e.getMessage());
        }
    }
}
