package vn.edu.vnu.auction.model.entity.factory;

import java.time.LocalDateTime;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.item.Other;

public class OtherFactory extends ItemFactory {

  public Item createItem(String itemName, int createdBy, String description, double startingPrice,
      LocalDateTime startingTime, LocalDateTime
          endTime) {
    return new Other(itemName, createdBy, description, startingPrice, startingTime, endTime);
  }
}
