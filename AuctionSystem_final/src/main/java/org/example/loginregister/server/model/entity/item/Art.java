package org.example.loginregister.server.model.entity.item;

import java.time.LocalDateTime;

public class Art extends Item {
    public Art( String itemName, String description, double startingPrice,  LocalDateTime startTime, java.time.LocalDateTime
            endTime){
        super( itemName, description, startingPrice, startTime, endTime);
    }
}
