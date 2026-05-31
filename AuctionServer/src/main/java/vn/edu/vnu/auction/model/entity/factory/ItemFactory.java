package vn.edu.vnu.auction.model.entity.factory;

import java.time.LocalDateTime;
import vn.edu.vnu.auction.model.entity.item.Item;

public abstract class ItemFactory {

  public abstract Item createItem(String itemName, int createdBy, String description,
      double startingPrice, LocalDateTime startingTime, LocalDateTime endTime);
}
