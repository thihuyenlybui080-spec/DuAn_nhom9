package model;
import observer.Subject;
import observer.Observer;
import java.util.ArrayList;
import java.util.List;
public class Auction implements Subject {
    private String id;
    private String itemName;
    private double currentPrice;
    private User highestBidder;
    private List<Observer> observers = new ArrayList<>();
    private List<Bid> bids = new ArrayList<>();
    public Auction(String id, String itemName, double startPrice) {
        this.id = id;
        this.itemName = itemName;
        this.currentPrice = startPrice;
    }
    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }
    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }
    @Override
    public void notifyObservers() {
        for (Observer o : observers) {
            o.update(id, currentPrice, highestBidder != null ? highestBidder.getName() : "None");
        }
    }
    public boolean placeBid(Bid bid) {
        if (bid.getAmount() <= currentPrice) {
            System.out.println("Bid that bai: gia phai cao hon gia hien tai!");
            return false;
        }
        currentPrice = bid.getAmount();
        highestBidder = bid.getBidder();
        bids.add(bid);
        System.out.println("Bid thanh cong: " + bid.getBidder().getName() + " da dat gia " + bid.getAmount());
        notifyObservers();
        return true;
    }
    public String getId() {
        return id;
    }
    public double getCurrentPrice() {
        return currentPrice;
    }
}