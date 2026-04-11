package models;

import base.Item;

import java.time.LocalDateTime;

public class Art extends Item {
    public Art(String id, String itemName, String description, double startingPrice, double currentPrice, LocalDateTime startTime, java.time.LocalDateTime
            endTime){
        super(id, itemName, description, startingPrice, currentPrice, startTime, endTime);
    }
}