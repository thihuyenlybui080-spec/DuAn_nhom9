package vn.edu.vnu.auction.model.factory;

import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.item.Vehicle;

import java.time.LocalDateTime;
public class VehicleFactory extends ItemFactory{
    public Item createItem(String itemName, String createdBy, String description, double startingPrice, LocalDateTime startingTime, LocalDateTime
            endTime){
        return new Vehicle( itemName, createdBy, description,startingPrice,  startingTime, endTime);
    }
}
