package vn.edu.vnu.auction.model.entity.factory;

import java.time.LocalDateTime;
import vn.edu.vnu.auction.model.entity.item.Art;
import vn.edu.vnu.auction.model.entity.item.Item;

public class ArtFactory extends ItemFactory {

  @Override
  public Item createItem(String itemName, int createdBy, String description, double startingPrice,
      LocalDateTime startingTime, LocalDateTime endTime) {
    return new Art(itemName, createdBy, description, startingPrice, startingTime, endTime);
  }
}
