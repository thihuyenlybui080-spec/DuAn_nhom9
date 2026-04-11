package models;

import base.Item;
import base.User;

import exceptions.AuthenticationException;

public class Admin extends User {
    public Admin(String userName, String id, String password, String email, String fullName) {
        super(userName, id, password, email, fullName);
    }

    public void monitorAuction(AuctionManager auctionManager) {
        System.out.println("Admin " + this.getName() + " is monitoring the list of auction sessions");
    }

    public void mangeUser(User user, String action) {
        System.out.println("Admin is performing " + action + " on user " + user.getName());
    }

    public void cancelAuction(Item item) {
        System.out.println("Admin has canceled the auction for item " + item.getName() + " due to violation");
        item.setStatus("CANCELED");//error handling will be needed if a bidder attempts to bid on a canceled item
    }

    @Override
    public void logIn(String name, String password) {
        if (!this.name.equals(name) || !this.password.equals(password)) {
            throw new AuthenticationException("Invalid username or password");
        }
    }
}