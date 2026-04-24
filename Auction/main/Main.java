package main;
import model.User;
import model.Auction;
import service.AuctionService;
import model.exception.InvalidBidException;
import model.exception.AuctionClosedException;

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
        try {
            service.placeBid("A1", u1, 1100);
            service.placeBid("A1", u2, 1200);
            service.placeBid("A1", u3, 1300);

            // test lỗi
            service.placeBid("A1", u1, 1000);

        } catch (InvalidBidException | AuctionClosedException e) {
            System.out.println("Loi: " + e.getMessage());
        }

        // kết thúc
        auction.finishAuction();

        try {
            service.placeBid("A1", u2, 1500);
        } catch (Exception e) {
            System.out.println("Loi sau khi dong: " + e.getMessage());
        }
    }
}
