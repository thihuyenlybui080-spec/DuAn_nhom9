package org.example.loginregister.server.model.factory;

import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.item.Vehicle;

import java.time.LocalDateTime;
public class VehicleFactory extends ItemFactory{
    @Override
    public Item createItem(String id, String itemName, String description, double startingPrice, double currentPrice, LocalDateTime startingTime, LocalDateTime
            endTime){
        return new Vehicle(id, itemName,description,startingPrice, currentPrice, startingTime, endTime);
    }
}
