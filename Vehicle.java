import java.time.LocalDateTime;

public class Vehicle extends Item{
    public Vehicle(String itemName, String description, double startingPrice, double currentPrice, LocalDateTime startTime, java.time.LocalDateTime
            endTime){
        super(itemName, description, startingPrice, currentPrice, startTime, endTime);
    }
}