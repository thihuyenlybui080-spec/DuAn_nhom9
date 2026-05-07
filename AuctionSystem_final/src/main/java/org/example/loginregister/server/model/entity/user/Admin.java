package org.example.loginregister.server.model.entity.user;


import org.example.loginregister.common.exception.AuthenticationException;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.util.AuctionManager;

public class Admin extends User {
    public Admin( String userName, String password, String email, String fullName) {
        super( userName, password, email, fullName);
    }


    public void monitorAuction(AuctionManager auctionManager) {
        System.out.println("Admin " + getName() + " is monitoring the list of auction sessions");
    }

    // Thay đổi trạng thái user
    public void manageUser(User user, UserStatus status) {
        user.updateStatus( new UserStatusRecord(status, this));
        System.out.println("[Admin] " + getName() + " set user " + user.getName() + " status to " + status);
    }


    public void cancelAuction(Auction auction) {
        System.out.println("Admin has canceled the auction for item " + auction.getItem().getItemName() + " due to violation");
        AuctionManager amg =AuctionManager.getInstance();
        amg.cancelAuction(auction.getId());
    }


}
