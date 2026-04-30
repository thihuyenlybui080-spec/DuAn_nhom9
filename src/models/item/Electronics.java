package models.item;

import base.Item;

import java.time.LocalDateTime;

public class Electronics extends Item {
    private final String brand;
    private final int warrantyMonths;

    public Electronics(String id, String itemName, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime){
        super(id, itemName, description, startingPrice, startTime, endTime);

    }

}