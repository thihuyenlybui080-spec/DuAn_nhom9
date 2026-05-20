package org.example.loginregister.server.dao;

import org.example.loginregister.server.database.DatabaseConfig;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;
import org.example.loginregister.server.model.entity.user.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    private static final String AUCTION_SELECT =
            "SELECT a.id               AS auction_id, "
            + "       a.status           AS auction_status, "
            + "       a.current_price    AS auction_current_price, "
            + "       a.highest_bidder_id, "
            + "       i.id               AS item_id, "
            + "       i.item_name, "
            + "       i.description, "
            + "       i.item_type, "
            + "       i.starting_price, "
            + "       i.start_time, "
            + "       i.end_time, "
            + "       i.created_by       AS seller_id, "
            + "       u.username         AS seller_name, "
            + "       u.password         AS seller_pass, "
            + "       u.email            AS seller_email, "
            + "       u.full_name        AS seller_fullname "
            + "FROM auctions a "
            + "JOIN items i ON a.item_id = i.id "
            + "JOIN users u ON i.created_by = u.id ";

    /** Lấy tất cả auction đang OPEN hoặc RUNNING. */
    public static List<Auction> getActiveAuctions(List<User> allUsers) {
        List<Auction> list = new ArrayList<>();
        String sql = AUCTION_SELECT + "WHERE a.status IN ('OPEN','RUNNING') ORDER BY a.end_time ASC";
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
        String sql = AUCTION_SELECT + "ORDER BY a.id DESC";
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
        String sql = AUCTION_SELECT + "WHERE i.created_by = ? ORDER BY a.id DESC";
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
    public static int insertAuction(int itemId, double startingPrice, long durationSeconds, java.time.LocalDateTime endTime) {
        String sql = "INSERT INTO auctions (item_id, status, current_price, duration_seconds, end_time) "
                + "VALUES (?, 'OPEN', ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, itemId);
            ps.setDouble(2, startingPrice);
            ps.setLong(3, durationSeconds);
            ps.setTimestamp(4, java.sql.Timestamp.valueOf(endTime));
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

    /** Cập nhật trạng thái auction (FINISHED / CANCELED / PAID). */
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

    /** Lấy DB id (int) từ string id dạng "auction-5" hoặc "item-3". */
    public static int parseDbId(String entityId) {
        try {
            String[] parts = entityId.split("-");
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (Exception e) {
            return -1;
        }
    }

    private static Auction mapAuction(ResultSet rs, List<User> allUsers) throws SQLException {
        String status = rs.getString("auction_status");

        Seller seller = new Seller(
                rs.getString("seller_name"),
                rs.getString("seller_pass"),
                rs.getString("seller_email"),
                rs.getString("seller_fullname")
        );
        seller.setId(String.valueOf(rs.getInt("seller_id")));

        Item item = ItemDAO.mapItem(rs, seller);

        Auction auction = new Auction(item);
        auction.setId("auction-" + rs.getInt("auction_id"));
        auction.setCurrentPrice(rs.getDouble("auction_current_price"));
        auction.setSeller(seller);

        int highestBidderId = rs.getInt("highest_bidder_id");
        if (!rs.wasNull() && allUsers != null) {
            allUsers.stream()
                    .filter(u -> u.getId().equals(String.valueOf(highestBidderId)))
                    .filter(u -> u instanceof Bidder)
                    .findFirst()
                    .ifPresent(u -> {
                        auction.setHighestBidder((Bidder) u);
                        auction.setHighestBidderName(u.getName());
                    });
        }

        try {
            auction.setStatus(AuctionStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            auction.setStatus(AuctionStatus.OPEN);
        }

        return auction;
    }
    /** Lấy auction theo ID từ database. */
    public static Auction getAuctionById(String auctionId) {
        int dbId = parseDbId(auctionId);
        if (dbId < 0) {
            return null;
        }
        
        String sql = AUCTION_SELECT + "WHERE a.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dbId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAuction(rs, null);
                }
            }
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] getAuctionById: " + e.getMessage());
        }
        return null;
    }
}
