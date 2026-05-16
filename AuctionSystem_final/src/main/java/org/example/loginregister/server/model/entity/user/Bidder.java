package org.example.loginregister.server.model.entity.user;

import org.example.loginregister.common.exception.AuctionClosedException;
import org.example.loginregister.common.exception.InvalidBidException;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionResult;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.BidTransaction;
import org.example.loginregister.server.model.entity.auto_bidding.AutoBidAgent;
import org.example.loginregister.server.model.entity.auto_bidding.AutoBidConfig;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.service.BidService;
import org.example.loginregister.server.service.PaymentService;
import org.example.loginregister.server.util.AuctionHistoryManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Bidder extends User  {

    //lưu những bid thành công
    private final List<BidTransaction> history = new CopyOnWriteArrayList<>();
    //lưu auto bid
    private final Map<String, AutoBidAgent> agents = new ConcurrentHashMap<>();
    //ds lưu những auction mà bidder này thắng
    private final Map<String, AuctionResult> wonAuctions = new ConcurrentHashMap<>();

    public Bidder( String name, String password, String email, String fullName) {
        super( name, password, email, fullName);
    }

    /**
     * Bidder chủ động đặt giá vào một phiên đấu giá.
     */
    public boolean bid(Auction auction, double amount) throws InvalidBidException, AuctionClosedException {
        if (!isActive()) {
            throw new IllegalStateException("[Bidder] " + getName() + ": account is locked and cannot place bids");
        }
        if (auction == null) {
            throw new IllegalArgumentException("[Bidder] " + getName() + ": invalid auction");
        }
        return BidService.getInstance().placeBid(auction.getId(), this, amount);
    }

    //Lưu lịch sử giao dịch sau khi đặt giá thành công.
    public void recordBid(Item item, double amount) {
        if (!isActive()) throw new IllegalStateException("Account is locked and cannot place bids");
        history.add(new BidTransaction(this, item, amount));
        System.out.println(this.getName() + " placed a bid of " + amount + " for item " + item.getItemName());
    }

    // Cập nhật danh sách auction đã thắng từ AuctionHistoryManager
    public void refreshWonAuctions() {
        wonAuctions.clear();
        List<AuctionResult> allResults = AuctionHistoryManager.getInstance().getAllResults();

        for (AuctionResult result : allResults) {
            if (result.getWinner() != null && result.getWinner().equals(this)) {
                wonAuctions.put(result.getAuctionId(), result);
            }
        }
    }

    // Kiểm tra xem bidder có thắng phiên này không
    public boolean hasWonAuction(String auctionId) {
        return getWonAuction(auctionId).isPresent();
    }

    /** Thanh toán phiên đã thắng qua {@link PaymentService}. */
    public boolean payForAuction(String auctionId) {
        return PaymentService.getInstance().processPayment(this, auctionId);
    }

    //=====AUTO BIDDING====
    public void enableAutoBid(Auction auction, AutoBidConfig config) {
        AutoBidAgent existing = agents.get(auction.getId());
        if (existing != null) {
            existing.stop();
        }
        AutoBidAgent agent = new AutoBidAgent(this, auction,config);
        agents.put(auction.getId(), agent);
    }

    public void disableAutoBid(String auctionId) {
        AutoBidAgent agent = agents.remove(auctionId);
        if (agent != null) agent.stop();
        System.out.println("[AutoBid] " + getName() + " disabled auto-bid for auction " + auctionId);
    }

    /**=====GETTER===== */
    public List<BidTransaction> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public List<AuctionResult> getWonAuctions() {
        refreshWonAuctions();
        return new ArrayList<>(wonAuctions.values());
    }

    public Optional<AuctionResult> getWonAuction(String auctionId) {
        refreshWonAuctions();
        return Optional.ofNullable(wonAuctions.get(auctionId));
    }

    @Override
    protected String getIdPrefix(){
        return "bidder";
    }

    @Override
    public String getRole(){
        return "Bidder";
    }
}
