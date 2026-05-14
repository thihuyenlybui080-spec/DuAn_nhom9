package org.example.loginregister.server.model.entity.item;

import org.example.loginregister.server.model.entity.Entity;
import org.example.loginregister.server.model.entity.user.Seller;

import java.time.LocalDateTime;
public abstract class Item extends Entity {
    private String itemName;
    private Seller seller;
    private String description;
    private double startingPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    public Item(String itemName,Seller seller, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime){
        super();
        this.seller = seller;
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
        System.out.println(itemName + ": " + description + " - StartingPrice: " + startingPrice);
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public String getSellerId() {
        return this.seller.getId();
    }
}
