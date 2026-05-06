package org.example.loginregister.server.model.entity.user;

import org.example.loginregister.common.exception.AuthenticationException;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionResult;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.BidTransaction;
import org.example.loginregister.server.model.entity.auto_bidding.AutoBidAgent;
import org.example.loginregister.server.model.entity.auto_bidding.AutoBidConfig;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.util.AuctionHistoryManager;
import org.example.loginregister.server.util.AuctionManager;

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

    //hành động sau khi bị BAN hoặc DELETED
    @Override
    protected void onStatusChanged(UserStatus newStatus) {
        if (newStatus == UserStatus.BANNED || newStatus == UserStatus.DELETED) {

            // Huỷ tất cả bid đang chạy của bidder này
            AuctionManager.getInstance().getActiveAuctions().forEach(auction -> auction.cancelBidsFrom(this));

            System.out.println("[Bidder] " + getName() + " bị " + newStatus + ": tất cả bid đã bị huỷ");
        }
    }

    /**
     * Bidder chủ động đặt giá vào một phiên đấu giá.
     * Kiểm tra tài khoản trước, sau đó chuyển toàn bộ logic xuống AuctionManager.*/
    public boolean bid(Auction auction, double amount) {
        if (!isActive()) {
            throw new IllegalStateException("[Bidder] " + getName() + ": tài khoản bị khoá, không thể đặt bid");
        }
        if (auction == null) {
            throw new IllegalArgumentException("[Bidder] " + getName() + ": auction không hợp lệ");
        }
        return AuctionManager.getInstance().placeBid(auction.getId(), this, amount);
    }

    //Lưu lịch sử giao dịch sau khi đặt giá thành công.
    public void recordBid(Item item, double amount) {
        if (!isActive()) throw new IllegalStateException("Tài khoản bị khoá, không thể đặt bid");
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


    // Giả lập thanh toán cho phiên đấu giá đã thắng
    public boolean payForAuction(String auctionId) {
        Optional<AuctionResult> resultOpt = getWonAuction(auctionId);

        if (resultOpt.isEmpty()) {
            System.err.println("❌ Bạn không phải người thắng phiên này!");
            return false;
        }

        AuctionResult result = resultOpt.get();

        if (result.getStatus() == AuctionStatus.PAID) {
            System.out.println("✅ Phiên này đã được thanh toán trước đó.");
            return true;
        }

        if (result.getStatus() != AuctionStatus.FINISHED) {
            System.err.println("❌ Không thế thanh toán phiên bây giờ.");
            return false;
        }

        // Giả lập thanh toán
        System.out.println("💰 " + getName() + " đang thanh toán " + result.getFinalPrice()  + " cho item: " + result.getItem().getItemName());
        AuctionHistoryManager ahm=AuctionHistoryManager.getInstance();

        //thay đổi trạng thái của auction từ FINISHED->PAID
        ahm.updateStatus(auctionId,AuctionStatus.PAID);

        System.out.println("✅ Thanh toán thành công!");

        // Refresh lại danh sách
        refreshWonAuctions();
        return true;
    }






    //=====AUTO BIDDING====
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




    /**=====GETTER===== */
    public List<BidTransaction> getHistory() {
        return Collections.unmodifiableList(history);
    }

    // Lấy danh sách các phiên đã thắng
    public List<AuctionResult> getWonAuctions() {
        refreshWonAuctions(); // Luôn lấy dữ liệu mới nhất
        return new ArrayList<>(wonAuctions.values());
    }

    // Lấy thông tin chi tiết một phiên đã thắng theo auctionId
    public Optional<AuctionResult> getWonAuction(String auctionId) {
        refreshWonAuctions();
        return Optional.ofNullable(wonAuctions.get(auctionId));
    }
}
