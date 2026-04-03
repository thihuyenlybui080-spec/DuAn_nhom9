package main;
import model.User;
import model.Auction;
import service.AuctionService;

public class Main {
    public static void main(String[] args) {
        AuctionService service = new AuctionService();
        // Tạo auction
        service.createAuction("A1", "Laptop", 1000);
        Auction auction = service.getAuction("A1");
        // Tạo users
        User u1 = new User("U1", "A");
        User u2 = new User("U2", "B");
        User u3 = new User("U3", "C");
        // Đăng ký observer
        auction.addObserver(u1);
        auction.addObserver(u2);    
        auction.addObserver(u3);
        // Đặt giá
        service.placeBid("A1", u1, 1100.0);
        service.placeBid("A1", u2, 1200.0);
        service.placeBid("A1", u3, 1300.0); 
    }   
}
