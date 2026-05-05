package models.item;

import base.Item;

import java.time.LocalDateTime;

public class Electronics extends Item {
    public Electronics( String itemName, String description, double startingPrice,  LocalDateTime startTime, LocalDateTime endTime){
        super( itemName, description, startingPrice, startTime, endTime);
    }
}
