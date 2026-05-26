package vn.edu.vnu.auction.model.entity.item;

import java.time.LocalDateTime;

public class Other extends Item {
    public Other(String itemName, int createdBy, String description, double startingPrice, LocalDateTime startTime, LocalDateTime
            endTime) {
        super(itemName, createdBy, description, startingPrice, startTime, endTime);
    }

    @Override
    public String getCategory() {
        return "Other";
    }
}
