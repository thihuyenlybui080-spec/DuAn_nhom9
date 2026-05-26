package vn.edu.vnu.auction.model.entity.user;

import vn.edu.vnu.auction.model.entity.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Seller extends User {
    private static final Logger logger = LoggerFactory.getLogger(Seller.class);
    private List<Item> ownedItems;

    public Seller( String name, String password, String email, String fullName){
        super( name, password, email, fullName);
    }

    public Seller(int id, String name, String password, String email, String fullName){
        super(id, name, password, email, fullName);
    }

    public void setOwnedItems(List<Item> items) {
        this.ownedItems = items;
    }

    public List<Item> getOwnedItems() {
        return ownedItems;
    }

    @Override
    public String getRole(){
        return "Seller";
    }

}
