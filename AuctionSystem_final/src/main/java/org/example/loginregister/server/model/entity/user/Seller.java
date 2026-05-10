package org.example.loginregister.server.model.entity.user;

import org.example.loginregister.common.exception.AuthenticationException;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.util.AuctionManager;

import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;



public class Seller extends User {
    private static List<Item> ownedItems;

    public Seller( String name, String password, String email, String fullName){
        super( name, password, email, fullName);
        this.ownedItems = new ArrayList<>();
    }
    @Override
    public void onStatusChanged(UserStatus newStatus) {
        if (newStatus == UserStatus.BANNED || newStatus == UserStatus.DELETED) {

            AuctionManager mgr = AuctionManager.getInstance();

            mgr.getActiveAuctions().stream().filter(a -> ownedItems.contains(a.getItem()))
                    .forEach(a -> {
                        if (AuctionStatus.RUNNING == a.getStatus()) {
                            mgr.cancelAuction(a.getId());
                            System.out.println("  -> CANCELED auction: " + a.getId());
                        } else if (AuctionStatus.OPEN == a.getStatus()) {
                            mgr.removeAuction(a.getId());
                            System.out.println("  -> REMOVED OPEN auction: " + a.getId());
                        }
                    });
        }
    }

    public void addItem(Item item){
        if (!isActive()) throw new IllegalStateException("Account is locked and cannot list items");
        ownedItems.add(item);
        System.out.println("Added " + item.getItemName() + " to the auction list");
    }

    public static void deleteItem(Item item){
        //ktra seller có sở hữu item này không
        if (!ownedItems.contains(item)) throw new IllegalArgumentException("Seller does not own this item");

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
            throw new IllegalStateException("Cannot delete item: auction has already started");
        }
    }

    // Seller chủ động mở phiên đấu giá khi sẵn sàng
    public String listItemForAuction(Item item) {
        if (!ownedItems.contains(item))
            throw new IllegalArgumentException("You do not own this item");

        String auctionId = "AUC-" + item.getItemName() + "-" + System.currentTimeMillis();
        AuctionManager.getInstance().startAuction(auctionId, this,item);
        return auctionId;
    }

    @Override
    protected String getIdPrefix(){
        return "seller";
    }
    @Override
    public String getRole(){
        return "Seller";
    }

}

