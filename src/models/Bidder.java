package models;
import base.Item;
import base.User;
import exceptions.AuthenticationException;
import java.util.List;
import java.util.ArrayList;

public class Bidder extends User {
    private List<BidTransaction> history;

    public Bidder(String name, String id, String password, String email, String fullName){
        super(name, id, password, email, fullName);
        this.history  = new ArrayList<>();
    }

    public void placeBid(Item item, double amount){
        if (amount <= item.getCurrentPrice()){
            System.out.println("The bid amount must be higher than the current price!");
        }
        BidTransaction transaction = new BidTransaction(this, item, amount);
        history.add(transaction);
        System.out.println(this.getName() + " placed a bid of " + amount + " for item " + item);
    }

    public List<BidTransaction> getHistory(){
        return history;
    }

    @Override
    public void logIn(String name, String password) throws AuthenticationException {
        if(!this.userName.equals(name) || !this.password.equals(password)){
            throw new AuthenticationException("Invalid username or password");
        }
    }
}