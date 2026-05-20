package org.example.loginregister.server.dao;

import org.example.loginregister.server.database.DatabaseConfig;
import org.example.loginregister.server.model.entity.BidTransaction;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.Bidder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    /** Lưu một lần đặt giá vào bảng bids. */
    public static void insertBid(int auctionDbId, int bidderId, double amount) {
        String sql = "INSERT INTO bids (auction_id, bidder_id, amount) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionDbId);
            ps.setInt(2, bidderId);
            ps.setDouble(3, amount);
            int rowsAffected = ps.executeUpdate();
            System.out.println("[BidDAO] insertBid: auctionDbId=" + auctionDbId + ", bidderId=" + bidderId + ", amount=" + amount + ", rowsAffected=" + rowsAffected);
        } catch (SQLException e) {
            System.err.println("[BidDAO] insertBid ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Lấy lịch sử bid của một bidder. */
    public static List<BidTransaction> getBidHistory(int bidderId, Bidder bidder) {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT b.amount AS bid_amount, b.bid_time, "
                + "i.id AS item_id, i.item_name, i.item_type, i.description, "
                + "i.starting_price, i.start_time, i.end_time "
                + "FROM bids b "
                + "JOIN auctions a ON b.auction_id = a.id "
                + "JOIN items i ON a.item_id = i.id "
                + "WHERE b.bidder_id = ? ORDER BY b.bid_time DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bidderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Item item = ItemDAO.mapItem(rs, null);
                    BidTransaction tx = new BidTransaction(bidder, item, rs.getDouble("bid_amount"));
                    list.add(tx);
                }
            }
        } catch (SQLException e) {
            System.err.println("[BidDAO] getBidHistory: " + e.getMessage());
        }
        return list;
    }

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
            System.err.println("[BidDAO] insertBidTransaction: " + e.getMessage());
        }
    }
    /** Lấy lịch sử bid của một phiên đấu giá. */
    public static List<BidTransaction> getBidsByAuction(int auctionDbId) {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT u.username, u.full_name, b.amount, b.bid_time "
                + "FROM bids b "
                + "JOIN users u ON b.bidder_id = u.id "
                + "WHERE b.auction_id = ? "
                + "ORDER BY b.bid_time DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionDbId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Bidder bidder = new Bidder(
                            rs.getString("username"),
                            "",
                            "",
                            rs.getString("full_name")
                    );
                    BidTransaction tx = new BidTransaction(bidder, null, rs.getDouble("amount"));
                    tx.setTimestamp(rs.getTimestamp("bid_time").toLocalDateTime());
                    list.add(tx);
                }
            }
        } catch (SQLException e) {
            System.err.println("[BidDAO] getBidsByAuction: " + e.getMessage());
        }
        return list;
    }
}
