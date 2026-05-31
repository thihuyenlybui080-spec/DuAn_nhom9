package vn.edu.vnu.auction.model.entity.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Admin extends User {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(Admin.class);

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
