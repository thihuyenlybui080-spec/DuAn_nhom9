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

    public Seller(String id, String name, String password, String email, String fullName){
        super(id, name, password, email, fullName);
        this.ownedItems = new ArrayList<>();
    }
    @Override
    protected void onStatusChanged(UserStatus newStatus) {
        if (newStatus == UserStatus.BANNED || newStatus == UserStatus.DELETED) {

            AuctionManager mgr = AuctionManager.getInstance();

            mgr.getActiveAuctions().stream().filter(a -> ownedItems.contains(a.getItem()))
                    .forEach(a -> {
                        if ("RUNNING".equals(a.getStatus())) {
                            a.setStatus(AuctionStatus.CANCELED);
                            System.out.println("  → CANCEL phiên: " + a.getId());
                        } else if ("OPEN".equals(a.getStatus())) {
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
        if (LocalDateTime.now().isBefore(item.getStartTime())){
            ownedItems.remove(item);
            System.out.println("Deleted product " + item.getItemName() + " from the auction list");
        }
    }


}

