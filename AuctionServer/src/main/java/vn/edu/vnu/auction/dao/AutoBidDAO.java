package vn.edu.vnu.auction.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.auction.database.DatabaseConfig;
import vn.edu.vnu.auction.model.entity.auto_bidding.AutoBidConfig;

/**
 * DAO cho auto-bid configuration.
 */
public class AutoBidDAO {

  private static final Logger logger = LoggerFactory.getLogger(AutoBidDAO.class);

  /**
   * Lưu hoặc update auto-bid configuration.
   */
  public static void saveAutoBid(int auctionDbId, int bidderDbId, double maxBid, double increment) {
    String sql = "INSERT INTO auto_bids (auction_id, bidder_id, max_bid, increment) " +
        "VALUES (?, ?, ?, ?) " +
        "ON CONFLICT(auction_id, bidder_id) DO UPDATE SET max_bid = excluded.max_bid, increment = excluded.increment";
    try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionDbId);
      ps.setInt(2, bidderDbId);
      ps.setDouble(3, maxBid);
      ps.setDouble(4, increment);
      ps.executeUpdate();
      ps.close();
      logger.info("Saved auto-bid: auctionId={}, bidderId={}", auctionDbId, bidderDbId);
    } catch (SQLException e) {
      logger.error("saveAutoBid ERROR", e);
    }
  }

  /**
   * Xóa auto-bid configuration.
   */
  public static void deleteAutoBid(int auctionDbId, int bidderDbId) {
    String sql = "DELETE FROM auto_bids WHERE auction_id = ? AND bidder_id = ?";
    try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionDbId);
      ps.setInt(2, bidderDbId);
      ps.executeUpdate();
      ps.close();
      logger.info("Deleted auto-bid: auctionId={}, bidderId={}", auctionDbId, bidderDbId);
    } catch (SQLException e) {
      logger.error("deleteAutoBid ERROR", e);
    }
  }

  /**
   * Lấy auto-bid configuration cho một auction.
   */
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
      ps.close();
    } catch (SQLException e) {
      logger.error("getAutoBidConfig ERROR", e);
    }
    return null;
  }

  /**
   * Lấy tất cả auto-bid configs theo auctionId
   */
  public static Map<Integer, AutoBidConfig> getAutoBidsByAuction(int auctionId) {
    Map<Integer, AutoBidConfig> configs = new HashMap<>();
    String sql = "SELECT bidder_id, max_bid, increment FROM auto_bids WHERE auction_id = ?";
    try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          int bidderId = rs.getInt("bidder_id");
          AutoBidConfig config = new AutoBidConfig(rs.getDouble("max_bid"),
              rs.getDouble("increment"));
          configs.put(bidderId, config);
        }
      }
    } catch (SQLException e) {
      logger.error("getAutoBidsByAuction ERROR", e);
    }
    return configs;
  }
}
