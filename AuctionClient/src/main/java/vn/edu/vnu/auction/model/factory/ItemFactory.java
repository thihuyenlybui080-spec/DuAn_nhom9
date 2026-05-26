package vn.edu.vnu.auction.model.factory;

import vn.edu.vnu.auction.model.entity.item.Item;

import java.time.LocalDateTime;

public abstract class ItemFactory {
    public abstract Item createItem(String itemName, int createdBy, String description, double startingPrice, LocalDateTime startingTime, LocalDateTime endTime);
}
