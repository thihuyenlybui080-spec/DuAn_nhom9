package org.example.loginregister.server.model.factory;

import org.example.loginregister.server.model.entity.item.Art;
import org.example.loginregister.server.model.entity.item.Item;

import java.time.LocalDateTime;

public class ArtFactory extends ItemFactory {
    @Override
    public Item createItem( String itemName, String description, double startingPrice, LocalDateTime startingTime, LocalDateTime endTime) {
        return new Art( itemName, description, startingPrice, startingTime, endTime);
    }
}
