import java.time.LocalDateTime;
public class ElectronicsFactory extends ItemFactory{
    @Override
    public Item createItem(String itemName,String description,double startingPrice,double currentPrice, LocalDateTime startingTime, LocalDateTime
            endTime){
        return new Electronics(itemName,description,startingPrice, currentPrice, startingTime, endTime);
    }
}