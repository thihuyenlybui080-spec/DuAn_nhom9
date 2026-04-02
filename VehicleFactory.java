import java.time.LocalDateTime;
public class VehicleFactory extends ItemFactory{
    @Override
    public Item createItem(String itemName,String description,double startingPrice,double currentPrice, LocalDateTime startingTime, LocalDateTime
            endTime){
        return new Vehicle(itemName,description,startingPrice, currentPrice, startingTime, endTime);
    }
}