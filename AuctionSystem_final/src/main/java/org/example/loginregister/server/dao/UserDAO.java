package org.example.loginregister.server.dao;

import org.example.loginregister.client.service.AuctionClientService;
import org.example.loginregister.server.database.DatabaseConfig;
import org.example.loginregister.server.model.entity.user.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /** Lấy toàn bộ danh sách user (Admin + Seller + Bidder). */
    public static List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, password, email, full_name, role FROM users";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] getAllUsers: " + e.getMessage());
        }
        return list;
    }

    /** Lấy user theo username và password (dùng cho login). */
    public static User getUserByCredentials(String username, String password) {
        String sql = "SELECT id, username, password, email, full_name, role FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] getUserByCredentials: " + e.getMessage());
        }
        return null;
    }

    /** Lấy user theo id. */
    public static User getUserById(int userId) {
        String sql = "SELECT id, username, password, email, full_name, role FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] getUserById: " + e.getMessage());
        }
        return null;
    }

    /** Kiểm tra username đã tồn tại chưa. */
    public static boolean isUsernameTaken(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] isUsernameTaken: " + e.getMessage());
        }
        return false;
    }

    /** Đăng ký user mới, trả về true nếu thành công. */
    public static boolean registerUser(String username, String password, String fullName,
                                       String email, String gender, String phone, String role) {
        String sql = "INSERT INTO users (username, password, email, full_name, gender, phone, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, email);
            ps.setString(4, fullName);
            ps.setString(5, gender);
            ps.setString(6, phone);
            ps.setString(7, role);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[UserDAO] registerUser: " + e.getMessage());
        }
        return false;
    }


    /** Map một dòng ResultSet → User (Bidder / Seller / Admin). */
    public static User mapUser(ResultSet rs) throws SQLException {
        String id       = String.valueOf(rs.getInt("id"));
        String username = rs.getString("username");
        String password = rs.getString("password");
        String email    = rs.getString("email");
        String fullName = rs.getString("full_name");
        String role     = rs.getString("role");

        User user;
        String prefix;
        switch (role) {
            case "SELLER": user = new Seller(username, password, email, fullName); prefix = "seller"; break;
            case "ADMIN":  user = new Admin(username, password, email, fullName);  prefix = "admin";  break;
            default:       user = new Bidder(username, password, email, fullName); prefix = "bidder";
        }
        user.setId(prefix + "-" + id);
        return user;
    }
}
