package factories;

import base.Item;
import models.item.Vehicle;

import java.time.LocalDateTime;
public class VehicleFactory extends ItemFactory{
    @Override
    public Item createItem( String itemName, String description, double startingPrice, LocalDateTime startingTime, LocalDateTime
            endTime){
        return new Vehicle( itemName,description,startingPrice, startingTime, endTime);
    }
}
