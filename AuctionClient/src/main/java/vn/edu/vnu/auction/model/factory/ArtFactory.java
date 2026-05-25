package vn.edu.vnu.auction.model.factory;

import vn.edu.vnu.auction.model.entity.item.Art;
import vn.edu.vnu.auction.model.entity.item.Item;

import java.time.LocalDateTime;

public class ArtFactory extends ItemFactory {
    @Override
    public Item createItem(String itemName, String createdBy, String description, double startingPrice, LocalDateTime startingTime, LocalDateTime endTime) {
        return new Art( itemName, createdBy, description, startingPrice, startingTime, endTime);
    }
}
