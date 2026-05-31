package vn.edu.vnu.auction.model.entity.user;

import java.io.Serial;
import java.util.List;
import vn.edu.vnu.auction.model.entity.item.Item;

public class Seller extends User {

  @Serial
  private static final long serialVersionUID = 1L;
  private List<Item> ownedItems;

  public Seller(String name, String password, String email, String fullName) {
    super(name, password, email, fullName);
  }

  public Seller(int id, String name, String password, String email, String fullName) {
    super(id, name, password, email, fullName);
  }

  public void setOwnedItems(List<Item> items) {
    this.ownedItems = items;
  }

  public List<Item> getOwnedItems() {
    return ownedItems;
  }

  @Override
  public String getRole() {
    return "Seller";
  }

}
