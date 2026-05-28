package vn.edu.vnu.auction.model.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.auction.common.exception.AuctionClosedException;
import vn.edu.vnu.auction.common.exception.InvalidBidException;
import vn.edu.vnu.auction.common.observer.Observer;
import vn.edu.vnu.auction.common.observer.Subject;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.model.entity.user.Seller;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

public class Auction implements Subject, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(Auction.class);

    // ===== FIELDS =====
    private int id;
    private Seller seller;
    private final Item item;
    private volatile double  currentPrice;
    private volatile Bidder  highestBidder;
    private volatile AuctionStatus status = AuctionStatus.OPEN;

    /**
     * ReentrantLock thay thế cho synchronized:
     *  - Cho phép tryLock() với timeout khi cần thiết.
     *  - Hỗ trợ fairness (true) để tránh thread starvation.
     *  - Dễ test/mock hơn synchronized.
     */
    private final ReentrantLock lock = new ReentrantLock(true); // fair lock

    /** CopyOnWriteArrayList để observer list không cần lock riêng khi iterate. */
    private transient final List<Observer> observers = new CopyOnWriteArrayList<>();

    /** Bid list chỉ được ghi bên trong lock nhưng serialize ghi ngoài lock nên dùng CopyOnWriteArrayList . */
    private final List<BidTransaction> bids = new CopyOnWriteArrayList<>();

    private transient volatile ScheduledFuture<?> currentTimer;



    // ===== CONSTRUCTOR =====
    public Auction(Item item) {
        this.id = item.getId();
        this.item= item;
        this.currentPrice = item.getStartingPrice();
    }



    // ===== OBSERVER (thread-safe via CopyOnWriteArrayList) =====
    @Override
    public void addObserver(Observer observer) {
        if (observer != null) {
            if (observers == null) {
                Field field;
                try {
                    field = Auction.class.getDeclaredField("observers");
                    field.setAccessible(true);
                    field.set(this, new CopyOnWriteArrayList<>());
                } catch (Exception e) {
                    logger.error("Failed to reinitialize observers", e);
                    return;
                }
            }
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(Observer observer) {
        if (observers != null) {
            observers.remove(observer);
        }
    }

    @Override
    public void notifyObservers() {
        if (observers == null) {
            return;
        }
        String bidderName = (highestBidder != null) ? highestBidder.getName() : "None";
        for (Observer o : observers) {
            o.update(id, currentPrice, bidderName);
        }
    }



    // ===== BIDDING =====

    /**
     * Thread-safe với ReentrantLock.
     * Mọi thay đổi trạng thái auction đều nằm trong lock.
     * @param notify whether to notify observers after bid is placed
     */
    public void placeBid(BidTransaction bid, boolean notify) throws InvalidBidException, AuctionClosedException {
        if (bid == null) throw new IllegalArgumentException("Invalid bid!");

        lock.lock();
        try {
            if (status == AuctionStatus.FINISHED ||status == AuctionStatus.CANCELED) {
                throw new AuctionClosedException("Auction is already closed!");
            }
            if (bid.getAmount() <= currentPrice) {
                throw new InvalidBidException("Bid amount must be greater than current price!");
            }

            currentPrice  = bid.getAmount();
            highestBidder = bid.getBidder();
            bids.add(bid);
        } finally {
            lock.unlock();
        }
        if (notify) {
            notifyObservers();
        }
    }

    // ===== TIMER / EXTENSION =====

    /**
     * Chỉ dùng nội bộ (Admin.cancelAuction).
     * Dùng lock để đảm bảo an toàn khi set status.
     */
    public void setStatus(AuctionStatus newStatus) {
        lock.lock();
        try {
            this.status = newStatus;
        } finally {
            lock.unlock();
        }
    }

    // ===== GETTERS =====

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setHighestBidder(Bidder highestBidder) {
        this.highestBidder = highestBidder;
    }
    public void setHighestBidderName(String highestBidderName){
        this.highestBidder.setName(highestBidderName);
    }
    public int getId() {
        return id;
    }
    public Item   getItem() {
        return item;
    }
    public double getCurrentPrice() {
        return currentPrice;
    }
    public Bidder getHighestBidder() {
        return highestBidder;
    }
    public AuctionStatus getStatus() {
        return status;
    }
    public List<BidTransaction> getBids() {
        return Collections.unmodifiableList(bids);
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public void setId(int id) {
        this.id = id;
    }
}
