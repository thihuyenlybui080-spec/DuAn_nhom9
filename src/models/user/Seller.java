package models.user;
import base.Item;
import base.User;
import exceptions.AuthenticationException;
import models.auction.AuctionStatus;
import models.manager.AuctionManager;

import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class Seller extends User {
    private List<Item> ownedItems;

    public Seller( String name, String password, String email, String fullName){
        super( name, password, email, fullName);
        this.ownedItems = new ArrayList<>();
    }
    @Override
    protected void onStatusChanged(UserStatus newStatus) {
        if (newStatus == UserStatus.BANNED || newStatus == UserStatus.DELETED) {

            AuctionManager mgr = AuctionManager.getInstance();

            mgr.getActiveAuctions().stream().filter(a -> ownedItems.contains(a.getItem()))
                    .forEach(a -> {
                        if (AuctionStatus.RUNNING == a.getStatus()) {
                            a.setStatus(AuctionStatus.CANCELED);
                            System.out.println("  → CANCEL phiên: " + a.getId());
                        } else if (AuctionStatus.OPEN == a.getStatus()) {
                            mgr.removeAuction(a.getId());
                            System.out.println("  → XOÁ phiên OPEN: " + a.getId());
                        }
                    });
        }
    }

    public void addItem(Item item){
        if (!isActive()) throw new IllegalStateException(
                "Tài khoản bị khoá, không thể đăng sản phẩm");
        ownedItems.add(item);
        System.out.println("Added " + item.getItemName() + " to the auction list");
    }

    public void deleteItem(Item item){
        //ktra seller có sở hữu item này không
        if (!ownedItems.contains(item)) throw new IllegalArgumentException("Seller không sở hữu item này");

        //chỉ xóa khi auction chưa bắt đầu
        if (LocalDateTime.now().isBefore(item.getStartTime())){

            //xóa auction chứa item khỏi ActiveAuctions
            AuctionManager mgr = AuctionManager.getInstance();
            mgr.getActiveAuctions().stream()
                    .filter(a -> a.getItem().equals(item))
                    .forEach(a -> mgr.removeAuction(a.getId()));

            ownedItems.remove(item);
            System.out.println("Deleted product " + item.getItemName() + " from the auction list");
        }
        else {
            throw new IllegalStateException("Không thể xoá: phiên đấu giá đã bắt đầu");
        }
    }
    // Seller chủ động mở phiên đấu giá khi sẵn sàng
    public String listItemForAuction(Item item) {
        if (!ownedItems.contains(item))
            throw new IllegalArgumentException("Không sở hữu item này");

        String auctionId = "AUC-" + item.getItemName() + "-" + System.currentTimeMillis();
        AuctionManager.getInstance().startAuction(auctionId, item);
        return auctionId;
    }

}

