package vn.edu.vnu.auction.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import vn.edu.vnu.auction.common.exception.DuplicateUsernameException;
import vn.edu.vnu.auction.database.DatabaseConfig;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.Admin;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.model.entity.user.Seller;
import vn.edu.vnu.auction.model.entity.user.User;
import vn.edu.vnu.auction.model.entity.user.UserStatus;
import vn.edu.vnu.auction.model.entity.user.UserStatusRecord;

public class UserDAO {

  /**
   * Lấy toàn bộ danh sách user (Admin + Seller + Bidder).
   */
  public static List<User> getAllUsers() {
    List<User> list = new ArrayList<>();
    ConcurrentHashMap<Integer, List<Item>> itemsMap = ItemDAO.getAllItemsGroupedByCreatedBy();
    String sql = "SELECT id, username, password, email, full_name, role, status FROM users";
    try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        User user = mapUser(rs);
        if (user instanceof Seller) {
          ((Seller) user).setOwnedItems(itemsMap.get(user.getId()));
        }
        list.add(user);
      }
      ps.close();
    } catch (SQLException e) {
      System.err.println("[UserDAO] getAllUsers: " + e.getMessage());
    }
    return list;
  }

  /**
   * Lấy user theo username và password (dùng cho login).
   */
  public static User getUserByCredentials(String username, String password) {
    String sql = "SELECT id, username, password, email, full_name, role, status FROM users WHERE username = ? AND password = ?";
    try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      ps.setString(2, password);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapUser(rs);
        }
      }
    } catch (SQLException e) {
      System.err.println("[UserDAO] getUserByCredentials: " + e.getMessage());
    }
    return null;
  }

  /**
   * Cập nhật status user trong DB
   */
  public static void updateUserStatus(int userId, UserStatus status) {
    String sql = "UPDATE users SET status = ? WHERE id = ?";
    try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, status.name());
      ps.setInt(2, userId);
      ps.executeUpdate();
    } catch (SQLException e) {
      System.err.println("[UserDAO] updateUserStatus: " + e.getMessage());
    }
  }

  /**
   * Lấy user theo id.
   */
  public static User getUserById(int userId) {
    String sql = "SELECT id, username, password, email, full_name, role, status FROM users WHERE id = ?";
    try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          User user = mapUser(rs);
          if (user instanceof Seller) {
            ((Seller) user).setOwnedItems(ItemDAO.getItemsBySeller(user.getId()));
          }
          return user;
        }
      }
    } catch (SQLException e) {
      System.err.println("[UserDAO] getUserById: " + e.getMessage());
    }
    return null;
  }

  /**
   * Kiểm tra username đã tồn tại chưa.
   */
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

  /**
   * Đăng ký user mới, throw DuplicateUsernameException nếu username đã tồn tại.
   */
  public static User registerUser(String username, String password, String fullName,
      String email, String gender, String phone, String role) throws DuplicateUsernameException {
    if (isUsernameTaken(username)) {
      throw new DuplicateUsernameException("Username '" + username + "' already exists");
    }
    Connection conn = null;
    try {
      conn = DatabaseConfig.getConnection();
      conn.setAutoCommit(false);
      String sql = "INSERT INTO users (username, password, email, full_name, gender, phone, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        ps.setString(1, username);
        ps.setString(2, password);
        ps.setString(3, email);
        ps.setString(4, fullName);
        ps.setString(5, gender);
        ps.setString(6, phone);
        ps.setString(7, role);
        ps.executeUpdate();

        try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
          if (generatedKeys.next()) {
            int dbId = generatedKeys.getInt(1);
            User user = switch (role) {
              case "SELLER" -> new Seller(dbId, username, password, email, fullName);
              case "ADMIN" -> new Admin(dbId, username, password, email, fullName);
              default -> new Bidder(dbId, username, password, email, fullName);
            };
            conn.commit();
            return user;
          }
        }
      }
    } catch (SQLException e) {
      System.err.println("[UserDAO] registerUser: " + e.getMessage());
      if (conn != null) {
        try {
          conn.rollback();
        } catch (SQLException ex) {
          System.err.println("[UserDAO] Rollback failed: " + ex.getMessage());
        }
      }
      throw new DuplicateUsernameException("Failed to register user: " + e.getMessage());
    } finally {
      if (conn != null) {
        try {
          conn.setAutoCommit(true);
          conn.close();
        } catch (SQLException e) {
          System.err.println("[UserDAO] Close connection failed: " + e.getMessage());
        }
      }
    }
    return null;
  }


  /**
   * Map một dòng ResultSet → User (Bidder / Seller / Admin).
   */
  public static User mapUser(ResultSet rs) throws SQLException {
    int id = rs.getInt("id");
    String username = rs.getString("username");
    String password = rs.getString("password");
    String email = rs.getString("email");
    String fullName = rs.getString("full_name");
    String role = rs.getString("role");
    String status = rs.getString("status");

    User user = switch (role) {
      case "SELLER" -> new Seller(id, username, password, email, fullName);
      case "ADMIN" -> new Admin(id, username, password, email, fullName);
      default -> new Bidder(id, username, password, email, fullName);
    };

    switch (status) {
      case "BANNED":
        user.updateStatus(new UserStatusRecord(UserStatus.BANNED, null));
        break;
      case "ACTIVE":
        user.updateStatus(new UserStatusRecord(UserStatus.ACTIVE, null));
        break;
      default:
        user.updateStatus(new UserStatusRecord(UserStatus.DELETED, null));
        break;
    }

    return user;
  }
}
