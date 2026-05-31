package vn.edu.vnu.auction.model.entity.auto_bidding;

import java.io.Serializable;
import java.time.LocalDateTime;
import vn.edu.vnu.auction.model.entity.user.Bidder;

public class AutoBid implements Serializable {

  private static final long serialVersionUID = 1L;
  private final Bidder bidder;
  private final int auctionId;
  private final AutoBidConfig config;
  private final LocalDateTime registeredAt;
  private volatile boolean active = true;

  public AutoBid(Bidder bidder, int auctionId, AutoBidConfig config) {
    this.bidder = bidder;
    this.auctionId = auctionId;
    this.config = config;
    this.registeredAt = LocalDateTime.now();
  }

  public Bidder getBidder() {
    return bidder;
  }

  public int getAuctionId() {
    return auctionId;
  }

  public AutoBidConfig getConfig() {
    return config;
  }

  public LocalDateTime getRegisteredAt() {
    return registeredAt;
  }

  public boolean isActive() {
    return active;
  }

  public void deactivate() {
    this.active = false;
  }
}