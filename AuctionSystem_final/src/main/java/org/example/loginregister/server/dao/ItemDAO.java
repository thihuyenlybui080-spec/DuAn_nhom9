package org.example.loginregister.server.dao;

import org.example.loginregister.server.database.DatabaseConfig;
import org.example.loginregister.server.model.entity.item.*;
import org.example.loginregister.server.model.entity.user.Seller;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    /** Lấy Item theo id DB. Trả null nếu không tìm thấy. */
    public static Item getItemById(int itemId, Seller seller) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapItemSingle(rs, seller);
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] getItemById: " + e.getMessage());
        }
        return null;
    }

    /** Lấy tất cả item của một seller. */
    public static List<Item> getItemsBySeller(int sellerId, Seller seller) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE created_by = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapItemSingle(rs, seller));
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] getItemsBySeller: " + e.getMessage());
        }
        return list;
    }

    /** Lưu item mới vào DB, trả về id được sinh ra. */
    public static int insertItem(Item item, int sellerId) {
        String sql = "INSERT INTO items (item_name, description, item_type, starting_price, current_price, start_time, end_time, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getItemName());
            ps.setString(2, item.getDescription());
            ps.setString(3, item.getCategory().toUpperCase());
            ps.setDouble(4, item.getStartingPrice());
            ps.setDouble(5, item.getStartingPrice());
            ps.setTimestamp(6, Timestamp.valueOf(item.getStartTime()));
            ps.setTimestamp(7, Timestamp.valueOf(item.getEndTime()));
            ps.setInt(8, sellerId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] insertItem: " + e.getMessage());
        }
        return -1;
    }

    /** Xóa item (chỉ khi chưa có auction liên quan). */
    public static boolean deleteItem(int itemId) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ItemDAO] deleteItem: " + e.getMessage());
            return false;
        }
    }

    /** Map từ JOIN query (dùng alias item_id). */
    public static Item mapItem(ResultSet rs, Seller seller) throws SQLException {
        int    id         = rs.getInt("item_id");
        String itemName   = rs.getString("item_name");
        String desc       = rs.getString("description");
        String itemType   = rs.getString("item_type");
        double startPrice = rs.getDouble("starting_price");
        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime   = rs.getTimestamp("end_time").toLocalDateTime();
        return buildItem(id, itemName, desc, itemType, startPrice, startTime, endTime, seller);
    }

    /** Map từ query đơn bảng items (cột id tên là "id"). */
    public static Item mapItemSingle(ResultSet rs, Seller seller) throws SQLException {
        int    id         = rs.getInt("id");
        String itemName   = rs.getString("item_name");
        String desc       = rs.getString("description");
        String itemType   = rs.getString("item_type");
        double startPrice = rs.getDouble("starting_price");
        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime   = rs.getTimestamp("end_time").toLocalDateTime();
        return buildItem(id, itemName, desc, itemType, startPrice, startTime, endTime, seller);
    }

    private static Item buildItem(int id, String itemName, String desc, String itemType,
                                  double startPrice, LocalDateTime startTime, LocalDateTime endTime,
                                  Seller seller) {
        Item item;
        switch (itemType) {
            case "ELECTRONICS": item = new Electronics(itemName, seller, desc, startPrice, startTime, endTime); break;
            case "VEHICLE":     item = new Vehicle(itemName, seller, desc, startPrice, startTime, endTime);     break;
            default:            item = new Art(itemName, seller, desc, startPrice, startTime, endTime);
        }
        item.setId("item-" + id);
        return item;
    }
}
