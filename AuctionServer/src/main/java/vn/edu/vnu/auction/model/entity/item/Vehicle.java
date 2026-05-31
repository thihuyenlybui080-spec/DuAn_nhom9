package vn.edu.vnu.auction.model.entity.item;


import java.time.LocalDateTime;

public class Vehicle extends Item {

  public Vehicle(String itemName, int createdBy, String description, double startingPrice,
      LocalDateTime startTime, LocalDateTime
          endTime) {
    super(itemName, createdBy, description, startingPrice, startTime, endTime);
  }

  public Vehicle(int id, String itemName, int createdBy, String description, double startingPrice,
      LocalDateTime startTime, LocalDateTime endTime) {
    super(id, itemName, createdBy, description, startingPrice, startTime, endTime);
  }

  @Override
  public String getCategory() {
    return "Vehicle";
  }
}
