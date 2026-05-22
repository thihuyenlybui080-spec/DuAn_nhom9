package org.example.loginregister.server.model.entity.user;
import org.example.loginregister.server.util.AuctionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Admin extends User {
    private static final Logger logger = LoggerFactory.getLogger(Admin.class);
    public Admin( String userName, String password, String email, String fullName) {
        super( userName, password, email, fullName);
    }

    public Admin(String id, String userName, String password, String email, String fullName) {
        super(id, userName, password, email, fullName);
    }

    public void monitorAuction(AuctionManager auctionManager) {
        logger.info("Admin {} is monitoring the list of auction sessions", getName());
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
