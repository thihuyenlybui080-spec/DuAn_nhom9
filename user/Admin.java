package user;
import javax.naming.AuthenticationException;

public class Admin extends User {
    public Admin(String userName, int id, String password, String email, String fullName){
        super(userName, id, password, email, fullName);
    }
    public void monitorAuction(AuctionManager auctionManager){
        System.out.println("Admin " + this.getName() + "dang kiem tra danh sach cac phien dau gia");
    }
    public void mangeUser(User user, String action){
        System.out.println("Admin dang thuc hien " + action + "voi nguoi dung " + user.getName());
    }
    public void cancelAuction(Item item){
        System.out.println("Admin da huy phien dau gia cho san pham " + item.getName() + "do vi pham");
        item.setStatus("CANCELED");//sau se phai xu li loi neu bidder co tinh dat gia vao canceled item
    }
    @Override
    public void logIn(String name, String password){
        if(!this.name.equals(name) || !this.password.equals(password)){
            throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu");
        }
    }
}
