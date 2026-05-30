package vn.edu.vnu.auction.model.entity.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.auction.model.entity.BidTransaction;
import vn.edu.vnu.auction.model.entity.item.Item;

import java.io.Serial;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Bidder extends User  {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(Bidder.class);
    private final List<BidTransaction> history = new CopyOnWriteArrayList<>();

    public Bidder( String name, String password, String email, String fullName) {
        super( name, password, email, fullName);
    }

    public Bidder(int id, String name, String password, String email, String fullName) {
        super(id, name, password, email, fullName);
    }

    public void recordBid(Item item, double amount) {
        if (!isActive()) throw new IllegalStateException("Account is locked and cannot place bids");
        history.add(new BidTransaction(this, item, amount));
        logger.info("{} placed a bid of {} for item {}", getName(), amount, item.getItemName());
    }


    @Override
    public String getRole(){
        return "Bidder";
    }
}
