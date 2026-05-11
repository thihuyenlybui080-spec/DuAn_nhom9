package org.example.loginregister.server.model.factory;

import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.item.Vehicle;

import java.time.LocalDateTime;
public class VehicleFactory extends ItemFactory{
    @Override
    public Item createItem( String itemName, String description, double startingPrice, LocalDateTime startingTime, LocalDateTime
            endTime){
        return new Vehicle( itemName,description,startingPrice,  startingTime, endTime);
    }
}
