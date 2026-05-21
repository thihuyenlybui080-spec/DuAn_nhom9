package org.example.loginregister.server.dao;

import org.example.loginregister.server.database.DatabaseConfig;
import org.example.loginregister.server.model.entity.BidTransaction;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.Bidder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    /** Lưu một lần đặt giá vào bảng bids với transaction handling để tránh race condition. */
    public static boolean insertBid(int auctionDbId, int bidderId, double amount) {
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

            // Lock auction row để tránh race condition
            String lockSql = "SELECT current_price FROM auctions WHERE id = ? FOR UPDATE";
            double currentPrice;
            try (PreparedStatement lockPs = conn.prepareStatement(lockSql)) {
                lockPs.setInt(1, auctionDbId);
                try (ResultSet rs = lockPs.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        System.err.println("[BidDAO] insertBid: Auction not found, auctionDbId=" + auctionDbId);
                        return false;
                    }
                    currentPrice = rs.getDouble("current_price");
                }
            }

            if (amount <= currentPrice) {
                conn.rollback();
                System.err.println("[BidDAO] insertBid: Bid amount " + amount + " not greater than current price " + currentPrice);
                return false;
            }

            String insertSql = "INSERT INTO bids (auction_id, bidder_id, amount) VALUES (?, ?, ?)";
            int insertRows = 0;
            try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                insertPs.setInt(1, auctionDbId);
                insertPs.setInt(2, bidderId);
                insertPs.setDouble(3, amount);
                insertRows = insertPs.executeUpdate();
                System.out.println("[BidDAO] insertBid: INSERT rowsAffected=" + insertRows);
            }

            String updateSql = "UPDATE auctions SET current_price = ?, highest_bidder_id = ?, status = 'RUNNING' WHERE id = ?";
            int updateRows = 0;
            try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                updatePs.setDouble(1, amount);
                updatePs.setInt(2, bidderId);
                updatePs.setInt(3, auctionDbId);
                updateRows = updatePs.executeUpdate();
                System.out.println("[BidDAO] insertBid: UPDATE rowsAffected=" + updateRows);
            }

            if (insertRows == 0 || updateRows == 0) {
                conn.rollback();
                System.err.println("[BidDAO] insertBid: No rows updated - insertRows=" + insertRows + ", updateRows=" + updateRows);
                return false;
            }

            conn.commit();
            System.out.println("[BidDAO] insertBid: SUCCESS - auctionDbId=" + auctionDbId + ", bidderId=" + bidderId + ", amount=" + amount);
            return true;
        } catch (SQLException e) {
            System.err.println("[BidDAO] insertBid ERROR: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[BidDAO] Rollback failed: " + ex.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("[BidDAO] Close connection failed: " + e.getMessage());
                }
            }
        }
    }

    /** Lấy lịch sử bid của một bidder. */
    public static List<BidTransaction> getBidHistory(int bidderId, Bidder bidder) {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT b.amount AS bid_amount, b.bid_time, b.auction_id, "
                + "i.id AS item_id, i.item_name, i.item_type, i.description, "
                + "i.starting_price, i.start_time, i.end_time, i.created_by, i.image_path, "
                + "u.username, u.full_name, u.email "
                + "FROM bids b "
                + "JOIN auctions a ON b.auction_id = a.id "
                + "JOIN items i ON a.item_id = i.id "
                + "JOIN users u ON b.bidder_id = u.id "
                + "WHERE b.bidder_id = ? ORDER BY b.bid_time DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bidderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Item item = ItemDAO.mapItem(rs);
                    Bidder bid = new Bidder(
                            rs.getString("username"),
                            "",
                            rs.getString("email"),
                            rs.getString("full_name")
                    );
                    bid.setId(String.valueOf(bidderId));
                    BidTransaction tx = new BidTransaction(bid, item, rs.getDouble("bid_amount"));
                    tx.setTimestamp(rs.getTimestamp("bid_time").toLocalDateTime());
                    tx.setAuctionId("auction-" + rs.getInt("auction_id"));
                    list.add(tx);
                }
            }
            System.out.println("[BidDAO] getBidHistory: Retrieved " + list.size() + " bids for bidderId=" + bidderId);
        } catch (SQLException e) {
            System.err.println("[BidDAO] getBidHistory: " + e.getMessage());
            e.printStackTrace();
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
