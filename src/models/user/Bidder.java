package models.user;

import base.Item;
import base.User;
import exceptions.AuthenticationException;
import models.auction.BidTransaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Bidder extends User {

    private final List<BidTransaction> history = new ArrayList<>();

    public Bidder(String id, String name, String password, String email, String fullName) {
        super(id, name, password, email, fullName);
    }

    //Lưu lịch sử giao dịch sau khi đặt giá thành công.
    public void recordBid(Item item, double amount) {
        BidTransaction transaction = new BidTransaction(this, item, amount);
        history.add(transaction);
        System.out.println(this.getName() + " placed a bid of " + amount + " for item " + item.getItemName());
    }

    public List<BidTransaction> getHistory() {
        return Collections.unmodifiableList(history);
    }

    @Override
    public void logIn(String name, String password) throws AuthenticationException {
        if (!this.userName.equals(name) || !this.password.equals(password)) {
            throw new AuthenticationException("Invalid username or password");
        }
    }
}
