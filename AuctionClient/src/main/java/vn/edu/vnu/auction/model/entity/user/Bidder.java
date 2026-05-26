package vn.edu.vnu.auction.model.entity.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.auction.model.entity.AuctionResult;
import vn.edu.vnu.auction.model.entity.BidTransaction;
import vn.edu.vnu.auction.model.entity.item.Item;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Bidder extends User  {
    private static final Logger logger = LoggerFactory.getLogger(Bidder.class);
    private final List<BidTransaction> history = new CopyOnWriteArrayList<>();
//    private final transient Map<Integer, AutoBidAgent> agents = new ConcurrentHashMap<>();
    private final Map<Integer, AuctionResult> wonAuctions = new ConcurrentHashMap<>();

    public Bidder( String name, String password, String email, String fullName) {
        super( name, password, email, fullName);
    }

    public Bidder(int id, String name, String password, String email, String fullName) {
        super(id, name, password, email, fullName);
    }

    /**
     * Bidder chủ động đặt giá vào một phiên đấu giá.
     */
//    public boolean bid(Auction auction, double amount) throws InvalidBidException, AuctionClosedException {
//        if (!isActive()) {
//            throw new IllegalStateException("[Bidder] " + getName() + ": account is locked and cannot place bids");
//        }
//        if (auction == null) {
//            throw new IllegalArgumentException("[Bidder] " + getName() + ": invalid auction");
//        }
//        return BidService.getInstance().placeBid(auction.getId(), this, amount);
//    }

    //Lưu lịch sử giao dịch sau khi đặt giá thành công.
    public void recordBid(Item item, double amount) {
        if (!isActive()) throw new IllegalStateException("Account is locked and cannot place bids");
        history.add(new BidTransaction(this, item, amount));
        logger.info("{} placed a bid of {} for item {}", getName(), amount, item.getItemName());
    }

    // Cập nhật danh sách auction đã thắng từ database
//    public void refreshWonAuctions() {
//        wonAuctions.clear();
//        int bidderDbId = Utils.parseDbId(this.getId());
//        if (bidderDbId < 0) {
//            return;
//        }
//
//        // Load all users to map auction data
//        List<User> allUsers = UserDAO.getAllUsers();
//        List<Auction> wonAuctionsList = AuctionDAO.getWonAuctionsByBidder(bidderDbId, allUsers);
//
//        for (Auction auction : wonAuctionsList) {
//            AuctionResult result = new AuctionResult(auction);
//            wonAuctions.put(result.getAuctionId(), result);
//        }
//    }

    // Kiểm tra xem bidder có thắng phiên này không
//    public boolean hasWonAuction(String auctionId) {
//        return getWonAuction(auctionId).isPresent();
//    }

    /** Thanh toán phiên đã thắng qua {@link PaymentService}. */
//    public boolean payForAuction(String auctionId) {
//        return PaymentService.getInstance().processPayment(this, auctionId);
//    }

    //=====AUTO BIDDING====
//    public void enableAutoBid(Auction auction, AutoBidConfig config) {
//        if (!isActive()) {
//            throw new IllegalStateException("[Bidder] " + getName() + ": account is locked and cannot enable auto-bid");
//        }
//        logger.debug("[Bidder] enableAutoBid called for {}", getName());
//        AutoBidAgent existing = agents.get(auction.getId());
//        if (existing != null) {
//            existing.stop();
//        }
//        AutoBidAgent agent = new AutoBidAgent(this, auction,config);
//        agents.put(auction.getId(), agent);
//        // Save to database for persistence
//        int auctionDbId = AuctionDAO.parseDbId(auction.getId());
//        int bidderDbId = AuctionDAO.parseDbId(this.getId());
//        logger.debug("[Bidder] auctionId={} -> auctionDbId={}, bidderId={} -> bidderDbId={}", auction.getId(), auctionDbId, this.getId(), bidderDbId);
//        if (auctionDbId > 0 && bidderDbId > 0) {
//            AutoBidDAO.saveAutoBid(auctionDbId, bidderDbId, config.getMaxBid(), config.getIncrement());
//        } else {
//            logger.error("[Bidder] FAILED: Invalid IDs");
//        }
//    }
//
//    public void disableAutoBid(String auctionId) {
//        AutoBidAgent agent = agents.remove(auctionId);
//        if (agent != null) agent.stop();
//        // Delete from database
//        int auctionDbId = AuctionDAO.parseDbId(auctionId);
//        int bidderDbId = AuctionDAO.parseDbId(this.getId());
//        if (auctionDbId > 0 && bidderDbId > 0) {
//            AutoBidDAO.deleteAutoBid(auctionDbId, bidderDbId);
//        }
//        logger.info("[AutoBid] {} disabled auto-bid for auction {}", getName(), auctionId);
//    }
//
//    /** Re-register auto-bid agent as observer when auction is reloaded from database. */
//    public void reRegisterAutoBidAgent(Auction auction) {
//        AutoBidAgent agent = agents.get(auction.getId());
//        if (agent != null) {
//            auction.addObserver(agent);
//            logger.info("Re-registered auto-bid agent for {} on auction {}", getName(), auction.getId());
//        }
//    }

    /**=====GETTER===== */
    public List<BidTransaction> getHistory() {
        return Collections.unmodifiableList(history);
    }

//    public List<AuctionResult> getWonAuctions() {
//        refreshWonAuctions();
//        return new ArrayList<>(wonAuctions.values());
//    }
//
//    public Optional<AuctionResult> getWonAuction(String auctionId) {
//        refreshWonAuctions();
//        return Optional.ofNullable(wonAuctions.get(auctionId));
//    }

    @Override
    public String getRole(){
        return "Bidder";
    }
}
