package models.user;

import base.User;
import exceptions.AuthenticationException;
import models.auction.Auction;
import models.manager.AuctionManager;

public class Admin extends User {
    public Admin(String id, String userName, String password, String email, String fullName) {
        super(id, userName, password, email, fullName);
    }

    public void monitorAuction(AuctionManager auctionManager) {
        System.out.println("Admin " + getName() + " is monitoring the list of auction sessions");
    }

    public void manageUser(User user, String action) {
        System.out.println("Admin is performing " + action + " on user " + user.getName());
    }


    public void cancelAuction(Auction auction) {
        System.out.println("Admin has canceled the auction for item "
                + auction.getItem().getItemName() + " due to violation");
        auction.setStatus(Auction.CANCELED);
    }

    @Override
    public void logIn(String name, String password) throws AuthenticationException {
        if (!this.userName.equals(name) || !this.password.equals(password)) {
            throw new AuthenticationException("Invalid username or password");
        }
    }
}
