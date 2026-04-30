package factories;

import base.Item;

import java.time.LocalDateTime;

public abstract class ItemFactory {
    public abstract Item createItem(String id, String itemName, String description, double startingPrice, LocalDateTime startingTime, LocalDateTime endTime);

}