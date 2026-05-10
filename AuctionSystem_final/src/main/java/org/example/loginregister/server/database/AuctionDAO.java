package org.example.loginregister.server.database;

import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.BidTransaction;
import org.example.loginregister.server.model.entity.item.Art;
import org.example.loginregister.server.model.entity.item.Electronics;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.item.Vehicle;
import org.example.loginregister.server.model.entity.user.Admin;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;
import org.example.loginregister.server.model.entity.user.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AuctionDAO – Tất cả truy vấn DB liên quan đến Auction, Item, Bid, User.
 * Dùng DatabaseConfig.getConnection() để lấy connection.
 */
public class AuctionDAO {

    // ================================================================
    //  USER
    // ================================================================

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
            System.err.println("[AuctionDAO] getAllUsers: " + e.getMessage());
        }
        return list;
    }

    /** Map một dòng ResultSet → User (Bidder / Seller / Admin). */
    private static User mapUser(ResultSet rs) throws SQLException {
        String id       = String.valueOf(rs.getInt("id"));
        String username = rs.getString("username");
        String password = rs.getString("password");
        String email    = rs.getString("email");
        String fullName = rs.getString("full_name");
        String role     = rs.getString("role");

        User user;
        switch (role) {
            case "SELLER":
                user = new Seller(username, password, email, fullName);
                break;
            case "ADMIN":
                user = new Admin(username, password, email, fullName);
                break;
            default:
                user = new Bidder(username, password, email, fullName);
        }
        user.setId(id);
        return user;
    }

    // ================================================================
    //  ITEM
    // ================================================================

    /** Lấy Item theo id DB. Trả null nếu không tìm thấy. */
    public static Item getItemById(int itemId, Seller seller) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapItem(rs, seller);
            }
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] getItemById: " + e.getMessage());
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
                while (rs.next()) list.add(mapItem(rs, seller));
            }
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] getItemsBySeller: " + e.getMessage());
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
            System.err.println("[AuctionDAO] insertItem: " + e.getMessage());
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
            System.err.println("[AuctionDAO] deleteItem: " + e.getMessage());
            return false;
        }
    }

    private static Item mapItem(ResultSet rs, Seller seller) throws SQLException {
        int    id           = rs.getInt("id");
        String itemName     = rs.getString("item_name");
        String description  = rs.getString("description");
        String itemType     = rs.getString("item_type");
        double startPrice   = rs.getDouble("starting_price");
        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime   = rs.getTimestamp("end_time").toLocalDateTime();

        Item item;
        switch (itemType) {
            case "ELECTRONICS": item = new Electronics(itemName, seller, description, startPrice, startTime, endTime); break;
            case "VEHICLE":     item = new Vehicle(itemName, seller, description, startPrice, startTime, endTime); break;
            default:            item = new Art(itemName, seller, description, startPrice, startTime, endTime);
        }
        item.setId("item-" + id);
        return item;
    }

    // ================================================================
    //  AUCTION
    // ================================================================

    /** Lấy tất cả auction đang OPEN hoặc RUNNING. */
    public static List<Auction> getActiveAuctions(List<User> allUsers) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.*, i.*, u.username AS seller_name, u.password AS seller_pass, "
                   + "u.email AS seller_email, u.full_name AS seller_fullname "
                   + "FROM auctions a "
                   + "JOIN items i ON a.item_id = i.id "
                   + "JOIN users u ON i.created_by = u.id "
                   + "WHERE a.status IN ('OPEN','RUNNING') "
                   + "ORDER BY a.end_time_millis ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Auction a = mapAuction(rs, allUsers);
                if (a != null) list.add(a);
            }
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] getActiveAuctions: " + e.getMessage());
        }
        return list;
    }

    /** Lấy tất cả auction (mọi trạng thái). */
    public static List<Auction> getAllAuctions(List<User> allUsers) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.*, i.*, u.username AS seller_name, u.password AS seller_pass, "
                   + "u.email AS seller_email, u.full_name AS seller_fullname "
                   + "FROM auctions a "
                   + "JOIN items i ON a.item_id = i.id "
                   + "JOIN users u ON i.created_by = u.id "
                   + "ORDER BY a.created_at DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Auction a = mapAuction(rs, allUsers);
                if (a != null) list.add(a);
            }
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] getAllAuctions: " + e.getMessage());
        }
        return list;
    }

    /** Lấy auction theo seller. */
    public static List<Auction> getAuctionsBySeller(int sellerId, List<User> allUsers) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.*, i.*, u.username AS seller_name, u.password AS seller_pass, "
                   + "u.email AS seller_email, u.full_name AS seller_fullname "
                   + "FROM auctions a "
                   + "JOIN items i ON a.item_id = i.id "
                   + "JOIN users u ON i.created_by = u.id "
                   + "WHERE i.created_by = ? "
                   + "ORDER BY a.created_at DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Auction a = mapAuction(rs, allUsers);
                    if (a != null) list.add(a);
                }
            }
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] getAuctionsBySeller: " + e.getMessage());
        }
        return list;
    }

    /** Lưu auction mới vào DB, trả về id được sinh ra. */
    public static int insertAuction(int itemId, double startingPrice, long durationSeconds, long endTimeMillis) {
        String sql = "INSERT INTO auctions (item_id, status, current_price, duration_seconds, end_time_millis) "
                   + "VALUES (?, 'OPEN', ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, itemId);
            ps.setDouble(2, startingPrice);
            ps.setLong(3, durationSeconds);
            ps.setLong(4, endTimeMillis);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] insertAuction: " + e.getMessage());
        }
        return -1;
    }

    /** Cập nhật giá và người dẫn đầu khi có bid mới. */
    public static void updateAuctionBid(int auctionDbId, double newPrice, int bidderId) {
        String sql = "UPDATE auctions SET current_price = ?, highest_bidder_id = ?, status = 'RUNNING' WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newPrice);
            ps.setInt(2, bidderId);
            ps.setInt(3, auctionDbId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] updateAuctionBid: " + e.getMessage());
        }
    }

    /** Cập nhật trạng thái auction (FINISHED / CANCELED). */
    public static void updateAuctionStatus(int auctionDbId, AuctionStatus status) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, auctionDbId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] updateAuctionStatus: " + e.getMessage());
        }
    }

    private static Auction mapAuction(ResultSet rs, List<User> allUsers) throws SQLException {
        int auctionDbId = rs.getInt("a.id");
        String status   = rs.getString("a.status");

        // Map seller
        String sellerName     = rs.getString("seller_name");
        String sellerPass     = rs.getString("seller_pass");
        String sellerEmail    = rs.getString("seller_email");
        String sellerFullname = rs.getString("seller_fullname");
        Seller seller = new Seller(sellerName, sellerPass, sellerEmail, sellerFullname);
        seller.setId(String.valueOf(rs.getInt("i.created_by")));

        // Map item
        Item item = mapItem(rs, seller);

        // Tạo Auction
        Auction auction = new Auction(seller, item);
        auction.setCurrentPrice(rs.getDouble("a.current_price"));

        // Map highest bidder nếu có
        int highestBidderId = rs.getInt("a.highest_bidder_id");
        if (!rs.wasNull() && allUsers != null) {
            allUsers.stream()
                    .filter(u -> u.getId().equals(String.valueOf(highestBidderId)))
                    .filter(u -> u instanceof Bidder)
                    .findFirst()
                    .ifPresent(u -> auction.setHighestBidderName(u.getName()));
        }

        // Set status
        try {
            auction.setStatus(AuctionStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            auction.setStatus(AuctionStatus.OPEN);
        }

        return auction;
    }

    // ================================================================
    //  BID
    // ================================================================

    /** Lưu một lần đặt giá vào bảng bids. */
    public static void insertBid(int auctionDbId, int bidderId, double amount) {
        String sql = "INSERT INTO bids (auction_id, bidder_id, amount) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionDbId);
            ps.setInt(2, bidderId);
            ps.setDouble(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] insertBid: " + e.getMessage());
        }
    }

    /** Lấy lịch sử bid của một bidder. */
    public static List<BidTransaction> getBidHistory(int bidderId, Bidder bidder) {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT b.amount, b.bid_time, i.item_name, i.item_type, i.description, "
                   + "i.starting_price, i.start_time, i.end_time, i.created_by "
                   + "FROM bids b "
                   + "JOIN auctions a ON b.auction_id = a.id "
                   + "JOIN items i ON a.item_id = i.id "
                   + "WHERE b.bidder_id = ? ORDER BY b.bid_time DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bidderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Item item = mapItem(rs, null);
                    BidTransaction tx = new BidTransaction(bidder, item, rs.getDouble("b.amount"));
                    list.add(tx);
                }
            }
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] getBidHistory: " + e.getMessage());
        }
        return list;
    }

    // ================================================================
    //  BID TRANSACTION (kết thúc auction)
    // ================================================================

    /** Ghi kết quả giao dịch khi auction kết thúc. */
    public static void insertBidTransaction(int auctionDbId, int bidderId, int itemDbId, double finalAmount) {
        String sql = "INSERT INTO bid_transactions (auction_id, bidder_id, item_id, final_amount) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionDbId);
            ps.setInt(2, bidderId);
            ps.setInt(3, itemDbId);
            ps.setDouble(4, finalAmount);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] insertBidTransaction: " + e.getMessage());
        }
    }

    // ================================================================
    //  HELPER
    // ================================================================

    /** Lấy DB id (int) từ string id dạng "auction-5" hoặc "item-3". */
    public static int parseDbId(String entityId) {
        try {
            String[] parts = entityId.split("-");
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (Exception e) {
            return -1;
        }
    }
}
