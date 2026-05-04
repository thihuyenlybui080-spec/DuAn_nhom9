package models.user;

import base.Item;
import base.User;
import exceptions.AuthenticationException;
import models.auction.Auction;
import models.auction.BidTransaction;
import models.auction.auto_bidding.AutoBidAgent;
import models.auction.auto_bidding.AutoBidConfig;
import models.manager.AuctionManager;
import observer.Observer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Bidder extends User  {

    private final List<BidTransaction> history = new CopyOnWriteArrayList<>();
    private final Map<String, AutoBidAgent> agents = new ConcurrentHashMap<>();


    public Bidder( String name, String password, String email, String fullName) {
        super( name, password, email, fullName);
    }

    @Override
    protected void onStatusChanged(UserStatus newStatus) {
        if (newStatus == UserStatus.BANNED || newStatus == UserStatus.DELETED) {

            // Huỷ tất cả bid đang chạy của bidder này
            AuctionManager.getInstance().getActiveAuctions().forEach(auction -> auction.cancelBidsFrom(this));

            System.out.println("[Bidder] " + getName() + " bị " + newStatus + ": tất cả bid đã bị huỷ");
        }
    }

    //Lưu lịch sử giao dịch sau khi đặt giá thành công.
    public void recordBid(Item item, double amount) {
        if (!isActive()) throw new IllegalStateException("Tài khoản bị khoá, không thể đặt bid");
        history.add(new BidTransaction(this, item, amount));
        System.out.println(this.getName() + " placed a bid of " + amount + " for item " + item.getItemName());
    }

    public List<BidTransaction> getHistory() {
        return Collections.unmodifiableList(history);
    }

    //autoBidding
    public void enableAutoBid(Auction auction, AutoBidConfig config) {
        // Dừng agent cũ nếu đã tồn tại cho phiên này
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
        System.out.println("[AutoBid] " + getName() + " đã tắt auto-bid cho phiên " + auctionId);
    }
}
