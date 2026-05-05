package models.item;

import base.Item;

import java.time.LocalDateTime;

public class Vehicle extends Item {
    public Vehicle( String itemName, String description, double startingPrice, LocalDateTime startTime, java.time.LocalDateTime endTime){
        super( itemName, description, startingPrice, startTime, endTime);
    }
}
