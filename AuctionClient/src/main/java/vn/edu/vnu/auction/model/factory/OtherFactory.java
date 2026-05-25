package vn.edu.vnu.auction.model.factory;

import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.item.Other;
import java.time.LocalDateTime;

public class OtherFactory extends ItemFactory {
    public Item createItem(String itemName, String createdBy, String description, double startingPrice, LocalDateTime startingTime, LocalDateTime
            endTime){
        return new Other( itemName, createdBy, description,startingPrice,  startingTime, endTime);
    }
}
