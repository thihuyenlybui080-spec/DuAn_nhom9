package org.example.loginregister.server.model.entity.item;

import org.example.loginregister.server.model.entity.Entity;
import org.example.loginregister.server.model.entity.user.Seller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
public abstract class Item extends Entity {
    private static final Logger logger = LoggerFactory.getLogger(Item.class);
    private String itemName;
    private String createdBy;
    private String description;
    private double startingPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String imagePath;
    public Item(String itemName, String createdBy, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime){
        super();
        this.createdBy = createdBy;
        this.itemName = itemName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    @Override
    protected String getIdPrefix() {
        return "item";
    }

    public String getItemName() {
        return itemName;
    }
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public double getStartingPrice() {
        return startingPrice;
    }
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    public abstract String getCategory();
    public void printInfo(){
        logger.info("{}: {} - StartingPrice: {}", itemName, description, startingPrice);
    }

    public String getSellerId() {
        return this.createdBy;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
