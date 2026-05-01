package org.example.loginregister.server.model.factory;


import org.example.loginregister.server.model.entity.item.Item;

import java.time.LocalDateTime;

public abstract class ItemFactory {
    public abstract Item createItem(String id, String itemName, String description, double startingPrice, double currentPrice, LocalDateTime startingTime, LocalDateTime
            endTime);
}
