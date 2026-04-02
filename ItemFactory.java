import java.time.LocalDateTime;

public abstract class ItemFactory {
    public Item listItem(String name,String description, double startingPrice,double currentPrice, LocalDateTime startingTime, LocalDateTime endTime){
        Item item = createItem(name, description, startingPrice,currentPrice,startingTime, endTime);
        item.setItemName(name);
        item.setDescription(description);
        item.setStartingPrice(startingPrice);
        item.setStartTime(startingTime);
        item.setEndTime(endTime);
        return item;

    }
    public abstract Item createItem(String itemName,String description,double startingPrice,double currentPrice, LocalDateTime startingTime, LocalDateTime
            endTime);
}