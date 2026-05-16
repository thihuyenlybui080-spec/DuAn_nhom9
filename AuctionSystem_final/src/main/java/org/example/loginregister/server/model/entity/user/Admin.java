package org.example.loginregister.server.model.entity.user;


import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.service.AuctionService;
import org.example.loginregister.server.service.UserService;
import org.example.loginregister.server.util.AuctionManager;

public class Admin extends User {
    public Admin( String userName, String password, String email, String fullName) {
        super( userName, password, email, fullName);
    }


    public void monitorAuction(AuctionManager auctionManager) {
        System.out.println("Admin " + getName() + " is monitoring the list of auction sessions");
    }

    /**
     * Thay đổi trạng thái user (ban, active, deleted) thông qua {@link UserService}.
     */
    public void manageUser(User user, UserStatus status) {
        UserService.getInstance().updateUserStatus(this, user, status);
    }


    public void cancelAuction(Auction auction) {
        System.out.println("Admin has canceled the auction for item " + auction.getItem().getItemName() + " due to violation");
        AuctionService.getInstance().cancelAuction(auction.getId());
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
