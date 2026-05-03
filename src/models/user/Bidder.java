package models.user;

import base.Item;
import base.User;
import exceptions.AuthenticationException;
import models.auction.BidTransaction;
import models.manager.AuctionManager;
import models.manager.AuctionManager;
import observer.Observer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Bidder extends User implements Observer {

    private final List<BidTransaction> history = new ArrayList<>();
    private final Map<String, AutoBidConfig> autoBidConfigs = new ConcurrentHashMap<>();


    public Bidder(String id, String name, String password, String email, String fullName) {
        super(id, name, password, email, fullName);
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
    // auto bid

    /*
      Đăng ký auto-bid cho một phiên đấu giá.
      Bidder tự addObserver vào Auction để nhận thông báo mỗi khi có bid mới.

      auction :phiên muốn tham gia auto-bid
      maxBid : giá tối đa sẵn sàng trả
      increment: bước giá mỗi lần tự đặt
     */
    public void enableAutoBid(Auction auction, double maxBid, double increment) {
        if (auction == null) throw new IllegalArgumentException("An auction cannot be null.");
        if (!isActive())     throw new IllegalStateException("The account has been locked; the automatic bidding feature cannot be activated.");

        AutoBidConfig config = new AutoBidConfig(maxBid, increment);
        autoBidConfigs.put(auction.getId(), config);
        auction.addObserver(this);      // Bidder lắng nghe auction này

        System.out.println("[AutoBid] " + getName() + "Auto-bid has been enabled for the auction " + auction.getId() + " | " + config);
    }

    /**
     * Huỷ đăng ký auto-bid cho một phiên.
     */
    public void disableAutoBid(Auction auction) {
        if (auction == null) return;
        autoBidConfigs.remove(auction.getId());
        auction.removeObserver(this);
        System.out.println("[AutoBid] " + getName() + " đã tắt auto-bid cho auction " + auction.getId());
    }

    /** Kiểm tra Bidder có đang bật auto-bid cho auction này không. */
    public boolean hasAutoBid(String auctionId) {
        return autoBidConfigs.containsKey(auctionId);
    }

    // ===== OBSERVER CALLBACK =====

    /**
     * Được gọi bởi Auction mỗi khi có bid mới (notifyObservers).
     *
     * Logic:
     * 1. Bỏ qua nếu Bidder này đang là người dẫn đầu (không cần tự outbid chính mình).
     * 2. Tính nextBid = currentPrice + increment.
     * 3. Nếu nextBid ≤ maxBid → đặt giá tự động.
     * 4. Nếu nextBid > maxBid → tắt auto-bid (đã đạt giới hạn).
     */
    @Override
    public void update(String auctionId, double newPrice, String highestBidderName) {
        AutoBidConfig config = autoBidConfigs.get(auctionId);
        if (config == null) return;  // không có auto-bid cho phiên này

        // Nếu mình đang là người dẫn đầu → không cần bid thêm
        if (getName().equals(highestBidderName)) return;

        double nextBid = newPrice + config.getIncrement();

        if (nextBid > config.getMaxBid()) {
            System.out.println("[AutoBid] " + getName() + " đã đạt maxBid (" + config.getMaxBid()+ ") tại auction " + auctionId + ". Tắt auto-bid.");
            // Lấy auction để removeObserver, không bắt buộc phải tắt tức thì
            autoBidConfigs.remove(auctionId);
            return;
        }

        // Đặt giá tự động qua AuctionManager (thread-safe, có anti-snipe)
        System.out.println("[AutoBid] " + getName() + " tự động đặt " + nextBid+ " cho auction " + auctionId);
        AuctionManager.getInstance().placeBid(auctionId, this, nextBid);
    }


}
