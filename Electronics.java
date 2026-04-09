import java.time.LocalDateTime;

public class Electronics extends Item{
    public Electronics(String id, String itemName, String description, double startingPrice, double currentPrice, LocalDateTime startTime, LocalDateTime endTime){
        super(id, itemName, description, startingPrice, currentPrice, startTime, endTime);
    }
}