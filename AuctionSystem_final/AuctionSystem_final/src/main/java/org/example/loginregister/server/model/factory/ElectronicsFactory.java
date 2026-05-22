package org.example.loginregister.server.model.factory;

import org.example.loginregister.server.model.entity.item.Electronics;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.Seller;

import java.time.LocalDateTime;
public class ElectronicsFactory extends ItemFactory{
    @Override
    public Item createItem(String itemName, String createdBy, String description, double startingPrice, LocalDateTime startingTime, LocalDateTime endTime){
        return new Electronics( itemName, createdBy, description, startingPrice, startingTime, endTime);
    }
}
