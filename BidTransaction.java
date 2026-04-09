import java.time.LocalDateTime;
public class BidTransaction {
    private Bidder bidder;
    private Item item;
    private double amount;
    private LocalDateTime timestamp;
    public BidTransaction(Bidder bidder, Item item, double amount){
        this.bidder = bidder;
        this.item = item;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }
    public Bidder getBidder(){
        return bidder;
    }
    public Item getItem(){
        return item;
    }
    public double getAmount(){
        return amount;
    }
    public LocalDateTime getTimestamp(){
        return timestamp;
    }//can 1 lop abstrct Enity de dinh nghiaa lai printInfo
    }

