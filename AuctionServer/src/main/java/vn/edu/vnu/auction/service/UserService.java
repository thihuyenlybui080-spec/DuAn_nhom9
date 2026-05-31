package vn.edu.vnu.auction.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.auction.ClientRegistry;
import vn.edu.vnu.auction.common.network.NotificationMessage;
import vn.edu.vnu.auction.dao.UserDAO;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.model.entity.user.Seller;
import vn.edu.vnu.auction.model.entity.user.User;
import vn.edu.vnu.auction.model.entity.user.UserStatus;
import vn.edu.vnu.auction.model.entity.user.UserStatusRecord;

/**
 * Dịch vụ quản lý người dùng: trạng thái tài khoản và hậu quả khi ban/xóa.
 */
public class UserService {

  private static final Logger logger = LoggerFactory.getLogger(UserService.class);
  private static volatile UserService instance;

  private final AuctionService auctionService;

  private UserService() {
    this.auctionService = AuctionService.getInstance();
  }

  /**
   * @return singleton {@link UserService}
   */
  public static UserService getInstance() {
    if (instance == null) {
      synchronized (UserService.class) {
        if (instance == null) {
          instance = new UserService();
        }
      }
    }
    return instance;
  }

  /**
   * Lấy toàn bộ user từ DB (admin).
   */
  public List<User> getAllUsers() {
    return UserDAO.getAllUsers();
  }

  public void toggleUserLock(User user) {
    if (user.isActive()) {
      user.updateStatus(new UserStatusRecord(UserStatus.BANNED, null));
      UserDAO.updateUserStatus(user.getId(), UserStatus.BANNED);

      handleUserRestricted(user);
      logger.info("User {} locked (BANNED)", user.getName());
    } else {
      user.updateStatus(UserStatusRecord.defaultActive());
      UserDAO.updateUserStatus(user.getId(), UserStatus.ACTIVE);
      handleUserUnrestricted(user);
      logger.info("User {} unlocked (ACTIVE)", user.getName());
    }
  }

  private void handleUserRestricted(User user) {
    if (user instanceof Bidder bidder) {
      handleBidderRestricted(bidder);
      notifyAutionsForLockedBidder(bidder);
    } else if (user instanceof Seller seller) {
      handleSellerRestricted(seller);
    }
  }

  private void handleUserUnrestricted(User user) {
    if (user instanceof Bidder bidder) {
      notifyAuctionsForUnlockedBidder(bidder);
    }
  }

  private void notifyAutionsForLockedBidder(Bidder bidder) {
    auctionService.getActiveAuctions().forEach(auction -> {
      ClientRegistry.getInstance().notifyAll(auction.getId(), new NotificationMessage(
          NotificationMessage.TYPE_USER_LOCKED,
          auction.getId(),
          bidder.getId()
      ));
    });
  }

  private void notifyAuctionsForUnlockedBidder(Bidder bidder) {
    auctionService.getActiveAuctions().forEach(auction -> {
      ClientRegistry.getInstance().notifyAll(auction.getId(), new NotificationMessage(
          NotificationMessage.TYPE_USER_UNLOCKED,
          auction.getId(),
          bidder.getId()
      ));
    });
  }

  private void handleBidderRestricted(Bidder bidder) {
    auctionService.getActiveAuctions().forEach(auction -> {
      auction.cancelBidsFrom(bidder);
      AutobidService.getInstance().disableAutoBid(auction.getId(), bidder.getId());
    });
    logger.info("Bidder {} set to {}: all active bids canceled and auto-bids disabled",
        bidder.getName(), UserStatus.BANNED);
  }

  public User getUserById(int userId) {
    return UserDAO.getUserById(userId);
  }

  private void handleSellerRestricted(Seller seller) {
    List<Auction> sellerAuctions = auctionService.getActiveAuctions().stream()
        .filter(a -> belongsToSeller(a, seller))
        .toList();

    for (Auction auction : sellerAuctions) {
      if (auction.getStatus() == AuctionStatus.RUNNING) {
        auctionService.cancelAuction(auction.getId());
        logger.info("Canceled running auction {} for restricted seller {}", auction.getId(),
            seller.getName());
      } else if (auction.getStatus() == AuctionStatus.OPEN) {
        auctionService.removeAuction(auction.getId());
        logger.info("Removed open auction {} for restricted seller {}", auction.getId(),
            seller.getName());
      }
    }
    logger.info("Seller {} set to {}: auctions processed", seller.getName(), UserStatus.BANNED);
  }

  private boolean belongsToSeller(Auction auction, Seller seller) {
    return auction.getSeller() != null
        && auction.getSeller().getId() == seller.getId();
  }

  public static synchronized void resetForTesting() {
    instance = null;
  }
}
