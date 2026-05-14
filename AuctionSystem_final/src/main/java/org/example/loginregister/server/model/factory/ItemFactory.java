package org.example.loginregister.server.model.factory;


import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.Seller;

import java.time.LocalDateTime;

public abstract class ItemFactory {
    public abstract Item createItem(String itemName, Seller seller, String description, double startingPrice, LocalDateTime startingTime, LocalDateTime endTime);
}
