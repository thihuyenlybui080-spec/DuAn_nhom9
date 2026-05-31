package vn.edu.vnu.auction.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.auction.database.DatabaseConfig;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.BidTransaction;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.model.entity.user.Seller;
import vn.edu.vnu.auction.model.entity.user.User;

public class AuctionDAO {

  private static final Logger logger = LoggerFactory.getLogger(AuctionDAO.class);

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
          + "       i.created_by, "
          + "       i.created_by       AS seller_id, "
          + "       i.image_path, "
          + "       u.username         AS seller_name, "
          + "       u.password         AS seller_pass, "
          + "       u.email            AS seller_email, "
          + "       u.full_name        AS seller_fullname "
          + "FROM auctions a "
          + "JOIN items i ON a.item_id = i.id "
          + "JOIN users u ON i.created_by = u.id ";

  /**
   * Lấy tất cả auction đang OPEN hoặc RUNNING.
   */
  public static List<Auction> getActiveAuctions(List<User> allUsers) {
    List<Auction> list = new ArrayList<>();
    String sql = AUCTION_SELECT + "WHERE a.status IN ('OPEN','RUNNING') ORDER BY a.end_time ASC";
    try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        Auction a = mapAuction(rs, allUsers);
          if (a != null) {
              list.add(a);
          }
      }
      ps.close();
    } catch (SQLException e) {
      System.err.println("[AuctionDAO] getActiveAuctions: " + e.getMessage());
    }
    return list;
  }

  /**
   * Lấy tất cả auction (mọi trạng thái).
   */
  public static List<Auction> getAllAuctions(List<User> allUsers) {
    List<Auction> list = new ArrayList<>();
    String sql = AUCTION_SELECT + "ORDER BY a.id DESC";
    try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        try {
          Auction a = mapAuction(rs, allUsers);
          if (a != null) {
            list.add(a);
          }
        } catch (Exception e) {
          logger.error("[AuctionDAO] Error mapping auction at row: {}", e.getMessage());
          e.printStackTrace();
        }
      }
      logger.info("[AuctionDAO] getAllAuctions: Loaded {} auctions", list.size());
    } catch (SQLException e) {
      logger.error("[AuctionDAO] getAllAuctions SQL error: {}", e.getMessage());
      e.printStackTrace();
    }
    return list;
  }

  /**
   * Lấy auction theo seller.
   */
  public static List<Auction> getAuctionsBySeller(int sellerId, List<User> allUsers) {
    List<Auction> list = new ArrayList<>();
    String sql = AUCTION_SELECT + "WHERE i.created_by = ? ORDER BY a.id DESC";
    try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sellerId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Auction a = mapAuction(rs, allUsers);
            if (a != null) {
                list.add(a);
            }
        }
      }
    } catch (SQLException e) {
      System.err.println("[AuctionDAO] getAuctionsBySeller: " + e.getMessage());
    }
    return list;
  }

  /**
   * Lưu auction mới vào DB, trả về id được sinh ra.
   */
  public static int insertAuction(int itemId, double startingPrice, long durationSeconds,
      java.time.LocalDateTime endTime) {
    String sql =
        "INSERT INTO auctions (item_id, status, current_price, duration_seconds, end_time) "
            + "VALUES (?, 'OPEN', ?, ?, ?)";
    try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, itemId);
      ps.setDouble(2, startingPrice);
      ps.setLong(3, durationSeconds);
      ps.setString(4, endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
          if (keys.next()) {
              return keys.getInt(1);
          }
      }
    } catch (SQLException e) {
      System.err.println("[AuctionDAO] insertAuction: " + e.getMessage());
    }
    return -1;
  }

  /**
   * Cập nhật giá và người dẫn đầu khi có bid mới.
   */
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

  /**
   * Cập nhật trạng thái auction (FINISHED / CANCELED / PAID).
   */
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
    String status = rs.getString("auction_status");

    Seller seller = new Seller(
        rs.getString("seller_name"),
        rs.getString("seller_pass"),
        rs.getString("seller_email"),
        rs.getString("seller_fullname")
    );
    seller.setId(rs.getInt("seller_id"));

    Item item = ItemDAO.mapItem(rs);

    Auction auction = new Auction(item);
    auction.setId(rs.getInt("auction_id"));
    auction.setCurrentPrice(rs.getDouble("auction_current_price"));
    auction.setSeller(seller);

    int highestBidderId = rs.getInt("highest_bidder_id");
    if (!rs.wasNull() && allUsers != null) {
      allUsers.stream()
          .filter(u -> u.getId() == highestBidderId && u instanceof Bidder)
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

    int auctionDbId = rs.getInt("auction_id");
    List<BidTransaction> bids =
        BidDAO.getBidsByAuction(auctionDbId);
    auction.addBids(bids);

    return auction;
  }

  /**
   * Lấy auction theo ID từ database.
   */
  public static Auction getAuctionById(int auctionId) {
    if (auctionId < 0) {
      return null;
    }

    String sql = AUCTION_SELECT + "WHERE a.id = ?";
    try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          List<User> allUsers = UserDAO.getAllUsers();
          return mapAuction(rs, allUsers);
        }
      }
    } catch (SQLException e) {
      System.err.println("[AuctionDAO] getAuctionById: " + e.getMessage());
    }
    return null;
  }

  public static void updateAuctionEndTime(int auctionId, LocalDateTime newEndTime) {
    String sql = "UPDATE auctions SET end_time = ? WHERE id = ?";
    try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, newEndTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
      ps.setInt(2, auctionId);
      ps.executeUpdate();
      logger.info("[AuctionDAO] Updated end_time for auction {} to {}", auctionId, newEndTime);
    } catch (SQLException e) {
      logger.error("[AuctionDAO] updateAuctionEndTime: {}", e.getMessage());
    }
  }

  /**
   * Lấy các auction đã thắng bởi bidder (status = FINISHED, PAID hoặc CANCELED).
   */
  public static List<Auction> getWonAuctionsByBidder(int bidderId, List<User> allUsers) {
    List<Auction> list = new ArrayList<>();
    String sql = AUCTION_SELECT
        + "WHERE a.highest_bidder_id = ? AND a.status IN ('FINISHED', 'PAID', 'CANCELED') ORDER BY a.id DESC";
    try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, bidderId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Auction a = mapAuction(rs, allUsers);
            if (a != null) {
                list.add(a);
            }
        }
      }
    } catch (SQLException e) {
      System.err.println("[AuctionDAO] getWonAuctionsByBidder: " + e.getMessage());
    }
    return list;
  }

  /**
   * Lấy các auction có status = FINISHED để khôi phục payment deadline khi server restart.
   */
  public static List<Auction> getFinishedAuctions(List<User> allUsers) {
    List<Auction> list = new ArrayList<>();
    String sql = AUCTION_SELECT + "WHERE a.status = 'FINISHED' ORDER BY a.id DESC";
    try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        Auction a = mapAuction(rs, allUsers);
          if (a != null) {
              list.add(a);
          }
      }
    } catch (SQLException e) {
      logger.error("[AuctionDAO] getFinishedAuctions SQL error: {}", e.getMessage());
    }
    return list;
  }
}
