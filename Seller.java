import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class Seller extends User {
    private List<Item> ownedItems;
    public Seller(String name, int id, String password, String email, String fullName){
        super(name, id, password, email, fullName);
        this.ownedItems = new ArrayList<>();
    }
    public void addItem(Item item){
        ownedItems.add(item);
        System.out.println("da them " + item.getName() + "vao danh sach dau gia");
    }
    public void deleteItem(Item item){
        if (LocalDateTime.now() < startTime){
            ownedItems.delete(item);
            System.out.println("da xoa san pham " + item.getName() + "khoi danh sach dau gia");
        }
    }

}

