package vn.edu.vnu.auction.model.entity.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.auction.model.entity.AuctionResult;
import vn.edu.vnu.auction.model.entity.BidTransaction;
import vn.edu.vnu.auction.model.entity.item.Item;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Bidder extends User  {
    private static final long serialVersionUID = 1L;

    public Bidder( String name, String password, String email, String fullName) {
        super( name, password, email, fullName);
    }

    public Bidder(int id, String name, String password, String email, String fullName) {
        super(id, name, password, email, fullName);
    }

    @Override
    public String getRole(){
        return "Bidder";
    }
}
