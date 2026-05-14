package org.example.loginregister.server.network;

import org.example.loginregister.common.observer.Observer;

import java.io.Serializable;

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

    /** tên hành động client muốn thực hiện */
    private final String action;

    /** dữ liệu kèm theo (object)*/
    private final Object data;

    /** Yêu cầu của client
     *
     * @param action hành động client thực hiện
     * @param data dữ liệu kèm theo
     */
    public Request(String action, Object data){
        this.action = action;
        this.data = data;
    }

    public String getAction() {
        return action;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString(){
        return "Request{action='" + action + "', data =" + data + "}";
    }
}
