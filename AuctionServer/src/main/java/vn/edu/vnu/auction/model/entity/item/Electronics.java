package vn.edu.vnu.auction.model.entity.item;

import java.time.LocalDateTime;

public class Electronics extends Item {

  public Electronics(String itemName, int createdBy, String description, double startingPrice,
      LocalDateTime startTime, LocalDateTime endTime) {
    super(itemName, createdBy, description, startingPrice, startTime, endTime);
  }

  public Electronics(int id, String itemName, int createdBy, String description,
      double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
    super(id, itemName, createdBy, description, startingPrice, startTime, endTime);
  }

  @Override
  public String getCategory() {
    return "Electronics";
  }
}