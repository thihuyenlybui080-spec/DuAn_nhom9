package org.example.loginregister.server.model.entity.user;
import org.example.loginregister.server.util.AuctionManager;

public class Admin extends User {
    public Admin( String userName, String password, String email, String fullName) {
        super( userName, password, email, fullName);
    }


    public void monitorAuction(AuctionManager auctionManager) {
        System.out.println("Admin " + getName() + " is monitoring the list of auction sessions");
    }

    @Override
    protected String getIdPrefix(){
        return "admin";
    }

    @Override
    public String getRole(){
        return "Admin";
    }
}
