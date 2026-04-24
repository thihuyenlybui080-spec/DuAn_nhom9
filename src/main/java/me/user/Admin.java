package me.user;


public class Admin extends User {
    public Admin(String id, String name, String password, String email, String fullName) {
        super(name, id, password, email, fullName);
    }

    public void monitorAuction(AuctionManager auctionManager) {
        System.out.println("Admin " + this.getName() + " is monitoring the list of auction sessions");
    }

    public void mangeUser(User user, String action) {
        System.out.println("Admin is performing " + action + " on user " + user.getName());
    }

    public void cancelAuction(Auction item) {
        System.out.println("Admin has canceled the auction for item " + item.getItem().getItemName() + " due to violation");
        item.setStatus("CANCELED");//error handling will be needed if a bidder attempts to bid on a canceled item
    }

    @Override
    public void logIn(String name, String password) throws AuthenticationException {
        System.out.println(this.userName);
        System.out.println(name);
        if (!this.userName.equals(name) || !this.password.equals(password)) {
            throw new AuthenticationException("Invalid username or password");
        }
    }
}