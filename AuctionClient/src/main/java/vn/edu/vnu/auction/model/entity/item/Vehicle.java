package vn.edu.vnu.auction.model.entity.item;


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
