package models;
import base.Item;
import base.User;
import exceptions.AuthenticationException;
import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class Seller extends User {
    private List<Item> ownedItems;

    public Seller(String name, String id, String password, String email, String fullName){
        super(name, id, password, email, fullName);
        this.ownedItems = new ArrayList<>();
    }

    public void addItem(Item item){
        ownedItems.add(item);
        System.out.println("Added " + item.getItemName() + " to the auction list");
    }

    public void deleteItem(Item item){
        if (LocalDateTime.now().isBefore(item.getStartTime())){
            ownedItems.remove(item);
            System.out.println("Deleted product " + item.getItemName() + " from the auction list");
        }
    }

    @Override
    public void logIn(String name, String password) throws AuthenticationException {
        if(!this.userName.equals(name) || !this.password.equals(password)){
            throw new AuthenticationException("Invalid username or password");
        }
    }
}

