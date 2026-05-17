package org.example.loginregister.server.model.entity.user;

import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.service.AuctionService;

import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;



public class Seller extends User {
    private List<Item> ownedItems;

    public Seller( String name, String password, String email, String fullName){
        super( name, password, email, fullName);
        this.ownedItems = new ArrayList<>();
    }

    public void addItem(Item item){
        if (!isActive()) throw new IllegalStateException("Account is locked and cannot list items");
        ownedItems.add(item);
        System.out.println("Added " + item.getItemName() + " to the auction list");
    }

    public void deleteItem(Item item){
        if (!ownedItems.contains(item)) throw new IllegalArgumentException("Seller does not own this item");

        if (LocalDateTime.now().isBefore(item.getStartTime())){
            AuctionService auctionService = AuctionService.getInstance();
            auctionService.getActiveAuctions().stream()
                    .filter(a -> a.getItem().equals(item))
                    .forEach(a -> auctionService.removeAuction(a.getId()));

            ownedItems.remove(item);
            System.out.println("Deleted product " + item.getItemName() + " from the auction list");
        }
        else {
            throw new IllegalStateException("Cannot delete item: auction has already started");
        }
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
