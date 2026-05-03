package factories;

import base.Item;
import models.item.Art;

import java.time.LocalDateTime;

public class ArtFactory extends ItemFactory {
    @Override
    public Item createItem(String id, String itemName, String description, double startingPrice, LocalDateTime startingTime, LocalDateTime endTime) {
        return new Art(id, itemName, description, startingPrice,  startingTime, endTime);
    }
}
