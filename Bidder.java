import java.util.List;
import java.util.ArrayList;
public class Bidder extends User {
    private List<BidTransaction> history;
    public Bidder(String name, int id, String password, String email, String fullName){
        super(name, id, password, email, fullName);
        this.history  = new ArrayList<>();
    }
    public void placeBid(Item item, double amount){
        if (amount <= item.getCurrentPrice){
            System.out.println("Gia dat phai cao hon gia hien tai!");
        }
        BidTransaction transaction = new BidTransaction(this, item, amount);
        history.add(transaction);
        System.out.println(this.getName() + "da dat gia " + amount + "cho san pham " + item);
        //can 1 lop abstrct Enity de dinh nghiaa lai printInfo
    }
    public List<BidTransaction> getHistory(){
        return history;
    }
}
