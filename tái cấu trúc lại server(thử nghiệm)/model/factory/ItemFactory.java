package org.example.loginregister.server.model.factory;


import org.example.loginregister.server.model.entity.item.Item;

import java.time.LocalDateTime;

public abstract class ItemFactory {
    public abstract Item createItem( String itemName, String description, double startingPrice,  LocalDateTime startingTime, LocalDateTime
            endTime);
}
