package vn.edu.vnu.auction.model.entity.factory;

import vn.edu.vnu.auction.model.entity.item.Electronics;
import vn.edu.vnu.auction.model.entity.item.Item;

import java.time.LocalDateTime;
public class ElectronicsFactory extends ItemFactory{
    @Override
    public Item createItem(String itemName, int createdBy, String description, double startingPrice, LocalDateTime startingTime, LocalDateTime endTime){
        return new Electronics( itemName, createdBy, description, startingPrice, startingTime, endTime);
    }
}
