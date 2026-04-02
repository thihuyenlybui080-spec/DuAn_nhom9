import java.time.LocalDateTime;

public class Art extends Item{
    public Art(String itemName, String description, double startingPrice, double currentPrice, LocalDateTime startTime, java.time.LocalDateTime
            endTime){
        super(itemName, description, startingPrice, currentPrice, startTime, endTime);
    }
}