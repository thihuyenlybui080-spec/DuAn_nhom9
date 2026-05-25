package vn.edu.vnu.auction.model.entity.item;

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
