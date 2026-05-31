package vn.edu.vnu.auction.common.network;

import java.io.Serializable;
import java.util.UUID;

/**
 * Đại diện cho một yêu cầu từ client gửi lên server.
 * <p> implements Serializable để có thể truyền Object
 * </p>
 */
public class Request implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String ACTION_LOGIN = "LOGIN";
  public static final String ACTION_REGISTER = "REGISTER";

  public static final String ACTION_GET_AUCTIONS = "GET_AUCTIONS";
  public static final String ACTION_GET_AUCTION_BY_ID = "GET_AUCTION_BY_ID";
  public static final String ACTION_PLACE_BID = "PLACE_BID";
  public static final String ACTION_GET_BID_HISTORY = "GET_BID_HISTORY";
  public static final String ACTION_WATCH_AUCTION = "WATCH_AUCTION";
  public static final String ACTION_LEAVE_AUCTION = "LEAVE_AUCTION";
  public static final String ACTION_GET_ALL_USERS = "GET_ALL_USERS";
  public static final String ACTION_FORCE_END_AUCTION = "FORCE_END_AUCTION";
  public static final String ACTION_CANCEL_AUCTION = "CANCEL_AUCTION";
  public static final String ACTION_GET_AUCTIONS_BY_SELLER = "GET_AUCTIONS_BY_SELLER";
  public static final String ACTION_TOGGLE_USER_LOCK = "TOGGLE_USER_LOCK";
  public static final String ACTION_GET_ITEMS_BY_SELLER = "GET_ITEMS_BY_SELLER";
  public static final String ACTION_CREATE_AUCTION_ITEM = "CREATE_AUCTION";
  public static final String ACTION_GET_BIDS_BY_AUCTION = "GET_BIDS_BY_AUCTION";
  public static final String ACTION_ENABLE_AUTO_BID = "ENABLE_AUTO_BID";
  public static final String ACTION_DISABLE_AUTO_BID = "DISABLE_AUTO_BID";
  public static final String ACTION_CHECK_AUTO_BID = "CHECK_AUTO_BID";
  public static final String ACTION_GET_BIDDER_HISTORY = "GET_BIDDER_HISTORY";
  public static final String ACTION_DELETE_ITEM = "DELETE_ITEM";
  public static final String ACTION_GET_WON_AUCTIONS = "GET_WON_AUCTIONS";
  public static final String ACTION_PAY_AUCTION = "PAY_AUCTION";
  public static final String ACTION_CREATE_PAYMENT_LINK = "CREATE_PAYMENT_LINK";


  /**
   * tên hành động client muốn thực hiện
   */
  private final String action;

  /**
   * dữ liệu kèm theo (object)
   */
  private final Object data;
  private final String requestId;

  /**
   * Yêu cầu của client
   *
   * @param action hành động client thực hiện
   * @param data   dữ liệu kèm theo
   */
  public Request(String action, Object data) {
    this.action = action;
    this.data = data;
    this.requestId = UUID.randomUUID().toString();
  }

  public String getAction() {
    return action;
  }

  public Object getData() {
    return data;
  }

  public String getRequestId() {
    return requestId;
  }

  @Override
  public String toString() {
    return "Request{action='" + action + "', data =" + data + "}";
  }
}
