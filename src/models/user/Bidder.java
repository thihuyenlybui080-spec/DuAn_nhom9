package models.user;

import base.Item;
import base.User;
import exceptions.AuthenticationException;
import models.auction.BidTransaction;
import models.manager.AuctionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Bidder extends User {

    private final List<BidTransaction> history = new ArrayList<>();

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


}
