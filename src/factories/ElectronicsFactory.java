package factories;

import base.Item;
import models.item.Electronics;

import java.time.LocalDateTime;
public class ElectronicsFactory extends ItemFactory{
    @Override
    public Item createItem(String id, String itemName, String description, double startingPrice, LocalDateTime startingTime, LocalDateTime
            endTime){
        return new Electronics(id, itemName,description,startingPrice, startingTime, endTime);
    }
}
