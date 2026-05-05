package org.example.loginregister.server.model.entity.item;

import java.time.LocalDateTime;

public class Electronics extends Item {
    public Electronics( String itemName, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime){
        super( itemName, description, startingPrice, startTime, endTime);
    }
}