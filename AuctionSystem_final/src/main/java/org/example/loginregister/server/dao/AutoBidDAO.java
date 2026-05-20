package org.example.loginregister.server.dao;

import org.example.loginregister.server.database.DatabaseConfig;
import org.example.loginregister.server.model.entity.auto_bidding.AutoBidConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO cho auto-bid configuration.
 */
public class AutoBidDAO {

    private static final Logger logger = LoggerFactory.getLogger(AutoBidDAO.class);

    /** Lưu hoặc update auto-bid configuration. */
    public static void saveAutoBid(int auctionDbId, int bidderDbId, double maxBid, double increment) {
        String sql = "INSERT INTO auto_bids (auction_id, bidder_id, max_bid, increment) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE max_bid = VALUES(max_bid), increment = VALUES(increment)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionDbId);
            ps.setInt(2, bidderDbId);
            ps.setDouble(3, maxBid);
            ps.setDouble(4, increment);
            ps.executeUpdate();
            logger.info("Saved auto-bid: auctionId={}, bidderId={}", auctionDbId, bidderDbId);
        } catch (SQLException e) {
            logger.error("saveAutoBid ERROR", e);
        }
    }

    /** Xóa auto-bid configuration. */
    public static void deleteAutoBid(int auctionDbId, int bidderDbId) {
        String sql = "DELETE FROM auto_bids WHERE auction_id = ? AND bidder_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionDbId);
            ps.setInt(2, bidderDbId);
            ps.executeUpdate();
            logger.info("Deleted auto-bid: auctionId={}, bidderId={}", auctionDbId, bidderDbId);
        } catch (SQLException e) {
            logger.error("deleteAutoBid ERROR", e);
        }
    }

    /** Lấy auto-bid configuration cho một auction. */
    public static AutoBidConfig getAutoBidConfig(int auctionDbId, int bidderDbId) {
        String sql = "SELECT max_bid, increment FROM auto_bids WHERE auction_id = ? AND bidder_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionDbId);
            ps.setInt(2, bidderDbId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AutoBidConfig(rs.getDouble("max_bid"), rs.getDouble("increment"));
                }
            }
        } catch (SQLException e) {
            logger.error("getAutoBidConfig ERROR", e);
        }
        return null;
    }
}
