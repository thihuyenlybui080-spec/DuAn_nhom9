import java.time.LocalDateTime;

public class ArtFactory extends ItemFactory {
    @Override
    public Item createItem(String id, String itemName, String description, double startingPrice, double currentPrice, LocalDateTime startingTime, LocalDateTime
            endTime) {
        return new Art(id, itemName, description, startingPrice, currentPrice, startingTime, endTime);
    }
}