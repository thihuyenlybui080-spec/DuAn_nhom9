package vn.edu.vnu.auction.model.entity.user;

import java.io.Serial;

public class Admin extends User {

  @Serial
  private static final long serialVersionUID = 1L;

  public Admin(String userName, String password, String email, String fullName) {
    super(userName, password, email, fullName);
  }

  public Admin(int id, String userName, String password, String email, String fullName) {
    super(id, userName, password, email, fullName);
  }

  @Override
  public String getRole() {
    return "Admin";
  }
}
