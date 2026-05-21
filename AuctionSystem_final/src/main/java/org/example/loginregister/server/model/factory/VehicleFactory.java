package org.example.loginregister.server.model.factory;

import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.item.Vehicle;
import org.example.loginregister.server.model.entity.user.Seller;

import java.time.LocalDateTime;
public class VehicleFactory extends ItemFactory{
    public Item createItem(String itemName, String createdBy, String description, double startingPrice, LocalDateTime startingTime, LocalDateTime
            endTime){
        return new Vehicle( itemName, createdBy, description,startingPrice,  startingTime, endTime);
    }
}
