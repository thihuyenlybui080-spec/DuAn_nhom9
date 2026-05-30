package vn.edu.vnu.auction.model.entity;

import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.User;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Đại diện cho kết quả của phiên đấu giá đã hoàn thành.
 * <p>
 * Chứa thông tin về kết quả đấu giá bao gồm người thắng,
 * giá cuối cùng, chi tiết sản phẩm, và lịch sử đặt giá.
 * </p>
 */
public class AuctionResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final Item item;
    private final User winner;
    private final double finalPrice;
    private final LocalDateTime endTime;
    private static List<BidTransaction> bidHistory;
    private  AuctionStatus status;

    /**
     * Tạo AuctionResult từ đối tượng Auction.
     *
     * @param auction phiên đấu giá để tạo kết quả
     */
    public AuctionResult(Auction auction) {
        this.auctionId = auction.getId();
        this.item = auction.getItem();
        this.winner = auction.getHighestBidder();
        this.finalPrice = auction.getCurrentPrice();
        this.status=auction.getStatus();
        this.endTime = auction.getItem().getEndTime();
        this.bidHistory = new ArrayList<>(auction.getBids());
    }

    /**
     * Đặt trạng thái cho kết quả đấu giá này.
     *
     * @param newStatus trạng thái mới cần đặt
     */
    public void setStatus(AuctionStatus newStatus){
        status=newStatus;
    }

    /**
     * Lấy ID phiên đấu giá.
     *
     * @return ID phiên đấu giá
     */
    public int getAuctionId() { return auctionId; }

    /**
     * Lấy sản phẩm được đấu giá.
     *
     * @return sản phẩm được đấu giá
     */
    public Item getItem() { return item; }

    /**
     * Lấy người thắng phiên đấu giá.
     *
     * @return người dùng thắng, hoặc null nếu không có người thắng
     */
    public User getWinner() { return winner; }

    /**
     * Lấy giá cuối cùng của phiên đấu giá.
     *
     * @return số tiền giá thắng cuối cùng
     */
    public double getFinalPrice() { return finalPrice; }

    /**
     * Lấy thời gian kết thúc của phiên đấu giá.
     *
     * @return thời gian khi phiên đấu giá kết thúc
     */
    public LocalDateTime getEndTime() { return endTime; }

    /**
     * Lấy lịch sử đặt giá cho phiên đấu giá này.
     *
     * @return danh sách giao dịch đặt giá
     */
    public static List<BidTransaction> getBidHistory() { return bidHistory; }

    /**
     * Lấy trạng thái hiện tại của kết quả đấu giá.
     *
     * @return trạng thái đấu giá
     */
    public AuctionStatus getStatus() {return status;}

    @Override
    public String toString() {
        String winnerName = (winner != null) ? winner.getName() : "None";

        return "Session " + auctionId + " | Item: " + item.getItemName() + " | Winner: " + winnerName + " | Final Price: " + (long) finalPrice;
    }
}
