package vn.edu.vnu.auction.model.entity.item;

import java.time.LocalDateTime;

public class Art extends Item {

  public Art(String itemName, int createdBy, String description, double startingPrice,
      LocalDateTime startTime, LocalDateTime
          endTime) {
    super(itemName, createdBy, description, startingPrice, startTime, endTime);
  }

  public Art(int id, String itemName, int createdBy, String description, double startingPrice,
      LocalDateTime startTime, LocalDateTime endTime) {
    super(id, itemName, createdBy, description, startingPrice, startTime, endTime);
  }

  @Override
  public String getCategory() {
    return "Art";
  }
}
