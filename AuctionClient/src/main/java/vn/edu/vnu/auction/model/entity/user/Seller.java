package vn.edu.vnu.auction.model.entity.user;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.auction.model.entity.item.Item;

public class Seller extends User {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(Seller.class);
  private List<Item> ownedItems;

  public Seller(String name, String password, String email, String fullName) {
    super(name, password, email, fullName);
  }

  public Seller(int id, String name, String password, String email, String fullName) {
    super(id, name, password, email, fullName);
  }

  public List<Item> getOwnedItems() {
    return ownedItems;
  }

  @Override
  public String getRole() {
    return "Seller";
  }

}
