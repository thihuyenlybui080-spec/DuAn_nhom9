package vn.edu.vnu.auction.model.entity;

import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.Bidder;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Đại diện cho một giao dịch đặt giá trong phiên đấu giá.
 * <p>
 * Ghi lại chi tiết của một lần đặt giá bao gồm người đặt giá, sản phẩm, số tiền,
 * thời gian, và ID phiên đấu giá liên quan.
 * </p>
 */
public class BidTransaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private Bidder bidder;
    private Item item;
    private double amount;
    private LocalDateTime timestamp;
    private int auctionId;

    /**
     * Tạo một BidTransaction mới.
     *
     * @param bidder người dùng đã đặt giá
     * @param item sản phẩm được đặt giá
     * @param amount số tiền đặt giá
     */
    public BidTransaction(Bidder bidder, Item item, double amount){
        this.bidder = bidder;
        this.item = item;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Lấy người đặt giá này.
     *
     * @return người đặt giá
     */
    public Bidder getBidder(){
        return bidder;
    }

    /**
     * Lấy sản phẩm được đặt giá.
     *
     * @return sản phẩm
     */
    public Item getItem(){
        return item;
    }

    /**
     * Lấy số tiền đặt giá.
     *
     * @return số tiền đặt giá
     */
    public double getAmount(){
        return amount;
    }

    /**
     * Lấy thời gian khi giá này được đặt.
     *
     * @return thời gian
     */
    public LocalDateTime getTimestamp(){
        return timestamp;
    }

    /**
     * Đặt thời gian cho giá này.
     *
     * @param timestamp thời gian cần đặt
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Lấy ID phiên đấu giá mà giá này thuộc về.
     *
     * @return ID phiên đấu giá
     */
    public int getAuctionId() {
        return auctionId;
    }

    /**
     * Đặt ID phiên đấu giá cho giá này.
     *
     * @param auctionId ID phiên đấu giá cần đặt
     */
    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }
}
