package vn.edu.vnu.auction.service;

import vn.edu.vnu.auction.dao.AuctionDAO;
import vn.edu.vnu.auction.dao.AutoBidDAO;
import vn.edu.vnu.auction.dao.UserDAO;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.user.Admin;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.model.entity.user.Seller;
import vn.edu.vnu.auction.model.entity.user.User;
import vn.edu.vnu.auction.model.entity.user.UserStatus;
import vn.edu.vnu.auction.model.entity.user.UserStatusRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.auction.util.Utils;

import java.util.List;

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

    /** Lấy toàn bộ user từ DB (admin). */
    public List<User> getAllUsers() {
        return UserDAO.getAllUsers();
    }

    /**
     * Cập nhật trạng thái user và xử lý side-effect (hủy bid, hủy auction...).
     *
     * @param admin  admin thực hiện thao tác
     * @param user   user bị thay đổi trạng thái
     * @param status trạng thái mới
     */
    public void updateUserStatus(Admin admin, User user, UserStatus status) {
        user.updateStatus(new UserStatusRecord(status, admin));
        applyStatusSideEffects(user, status);
        logger.info("Admin {} set user {} status to {}", admin.getName(), user.getName(), status);
    }

    public void applyStatusSideEffects(User user, UserStatus status) {
        if (status != UserStatus.BANNED && status != UserStatus.DELETED) {
            return;
        }
        if (user instanceof Bidder bidder) {
            handleBidderRestricted(bidder, status);
        } else if (user instanceof Seller seller) {
            handleSellerRestricted(seller, status);
        }
    }

     public User toggleUserLock(User user){
        if (user.isActive()) {
            user.updateStatus(new UserStatusRecord(UserStatus.BANNED, null));
            UserDAO.updateUserStatus(user.getId(), UserStatus.BANNED);
            if (user instanceof Bidder bidder) {
                handleBidderRestricted(bidder, UserStatus.BANNED);
            } else if (user instanceof Seller seller) {
                handleSellerRestricted(seller, UserStatus.BANNED);
            }
            logger.info("User {} locked (BANNED)", user.getName());
        }
        else {
            user.updateStatus(UserStatusRecord.defaultActive());
            UserDAO.updateUserStatus(user.getId(), UserStatus.ACTIVE);
            logger.info("User {} unlocked (ACTIVE)", user.getName());
        }
        return user;
     }

    private void handleBidderRestricted(Bidder bidder, UserStatus status) {
        //TODO: reimplement
//        auctionService.getActiveAuctions().forEach(auction -> {
//            auction.cancelBidsFrom(bidder);
//            // Disable auto-bid for this bidder on all auctions
//            bidder.disableAutoBid(auction.getId());
//            // Remove from database
//            int auctionDbId = Utils.parseDbId(auction.getId());
//            int bidderDbId = Utils.parseDbId(bidder.getId());
//            if(auctionDbId > 0 && bidderDbId > 0) {
//                AutoBidDAO.deleteAutoBid(auctionDbId, bidderDbId);
//            }
//        });
//        logger.info("Bidder {} set to {}: all active bids canceled and auto-bids disabled", bidder.getName(), status);
    }

    public User getUserById(String userId){
        User user = UserDAO.getUserById(Integer.parseInt(userId.split("-")[1]));
        return user;
    }

    private void handleSellerRestricted(Seller seller, UserStatus status) {
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
        logger.info("Seller {} set to {}: auctions processed", seller.getName(), status);
    }

    private boolean belongsToSeller(Auction auction, Seller seller) {
        return auction.getSeller() != null
                && seller.getId() != null
                && seller.getId().equals(auction.getSeller().getId());
    }

    public static synchronized void resetForTesting() {
        instance = null;
    }
}
