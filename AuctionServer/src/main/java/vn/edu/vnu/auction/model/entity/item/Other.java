package vn.edu.vnu.auction.model.entity.item;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Other extends Item implements Serializable {

  private static final long serialVersionUID = 1L;

  public Other(String itemName, int createdBy, String description, double startingPrice,
      LocalDateTime startTime, LocalDateTime
          endTime) {
    super(itemName, createdBy, description, startingPrice, startTime, endTime);
  }

  public Other(int id, String itemName, int createdBy, String description, double startingPrice,
      LocalDateTime startTime, LocalDateTime endTime) {
    super(id, itemName, createdBy, description, startingPrice, startTime, endTime);
  }

  @Override
  public String getCategory() {
    return "Other";
  }
}
