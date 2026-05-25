package vn.edu.vnu.auction.common.network;

import java.io.Serializable;

public class NotificationMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TYPE_BID_UPDATED = "BID_UPDATED";
    public static final String TYPE_AUCTION_STARTED = "AUCTION_STARTED";
    public static final String TYPE_AUCTION_ENDED = "AUCTION_ENDED";
    public static final String TYPE_TIME_EXTENDED = "TIME_EXTENDED";
    public static final String TYPE_AUTO_BID_AUCTION_ENDED = "AUTO_BID_AUCTION_ENDED";

    private final String type;
    private final String auctionId;

    private final Object data;

    public NotificationMessage(String type, String auctionId, Object data){
        this.type = type;
        this.auctionId = auctionId;
        this.data = data;
    }

    public String getType(){
        return type;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString(){
        return "Notification{type='" + type +"',auctionId='" + auctionId + "'}";
    }
}
