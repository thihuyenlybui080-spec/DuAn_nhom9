package org.example.loginregister.server.model.entity.user;

import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.service.AuctionService;
import org.example.loginregister.server.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;



public class Seller extends User {
    private static final Logger logger = LoggerFactory.getLogger(Seller.class);
    private List<Item> ownedItems;

    public Seller( String name, String password, String email, String fullName){
        super( name, password, email, fullName);
        //this.ownedItems = ItemService.getInstance().getItemsBySeller(this.getIdPrefix());
    }

    public Seller(String id, String name, String password, String email, String fullName){
        super(id, name, password, email, fullName);
        //this.ownedItems = ItemService.getInstance().getItemsBySeller(this.getId());
    }

    public void addItem(Item item){
        if (!isActive()) throw new IllegalStateException("Account is locked and cannot list items");
        if (ownedItems == null) ownedItems = new ArrayList<>();
        ownedItems.add(item);
        logger.info("Added {} to the auction list", item.getItemName());
    }

    public void deleteItem(Item item){
        if (!ownedItems.contains(item)) throw new IllegalArgumentException("Seller does not own this item");

        if (LocalDateTime.now().isBefore(item.getStartTime())){
            AuctionService auctionService = AuctionService.getInstance();
            auctionService.getActiveAuctions().stream()
                    .filter(a -> a.getItem().equals(item))
                    .forEach(a -> auctionService.removeAuction(a.getId()));

            ownedItems.remove(item);
            logger.info("Deleted product {} from the auction list", item.getItemName());
        }
        else {
            throw new IllegalStateException("Cannot delete item: auction has already started");
        }
    }

    public void setOwnedItems(List<Item> items) {
        this.ownedItems = items;
    }

    public List<Item> getOwnedItems() {
        return ownedItems;
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
