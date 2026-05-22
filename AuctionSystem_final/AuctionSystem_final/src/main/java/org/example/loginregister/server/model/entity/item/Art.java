package org.example.loginregister.server.model.entity.item;

import org.example.loginregister.server.model.entity.user.Seller;

import java.time.LocalDateTime;

public class Art extends Item {
    public Art(String itemName, String createdBy, String description, double startingPrice, LocalDateTime startTime, java.time.LocalDateTime
            endTime){
        super( itemName, createdBy, description, startingPrice, startTime, endTime);
    }
    @Override
    public String getCategory(){
        return "Art";
    }
}
