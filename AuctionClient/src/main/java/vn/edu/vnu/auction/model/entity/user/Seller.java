package vn.edu.vnu.auction.model.entity.user;

import java.io.Serial;

public class Seller extends User {

  @Serial
  private static final long serialVersionUID = 1L;

  public Seller(String name, String password, String email, String fullName) {
    super(name, password, email, fullName);
  }

  public Seller(int id, String name, String password, String email, String fullName) {
    super(id, name, password, email, fullName);
  }

  @Override
  public String getRole() {
    return "Seller";
  }

}
