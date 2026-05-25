package org.example.loginregister.server.model.entity.item;


import org.example.loginregister.server.model.entity.user.Seller;

import java.time.LocalDateTime;

public class Vehicle extends Item {
    public Vehicle(String itemName, String createdBy, String description, double startingPrice, LocalDateTime startTime, LocalDateTime
            endTime){
        super( itemName, createdBy, description, startingPrice, startTime, endTime);
    }
    @Override
    public String getCategory(){
        return "Vehicle";
    }
}
