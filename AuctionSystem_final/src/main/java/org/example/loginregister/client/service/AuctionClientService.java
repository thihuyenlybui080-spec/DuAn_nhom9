package org.example.loginregister.client.service;

import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.BidTransaction;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.User;
import org.example.loginregister.server.common.network.Request;
import org.example.loginregister.server.common.network.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Giao tiếp với server qua socket
 */
public class AuctionClientService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionClientService.class.getName());
    private static AuctionClientService instance;

    public static AuctionClientService getInstance() {
        if (instance == null) {
            instance = new AuctionClientService();
        }
        return instance;
    }

    private AuctionClientService() {
    }

    /**
     * Đăng nhập một tài khoản đã có
     *
     * @param username tên đăng nhập
     * @param password mật khẩu
     * @return
     */
    public User login(String username, String password) {
        Map<String, String> data = new HashMap<>();

        data.put("username", username);
        data.put("password", password);

        Response response = sendRequest(new Request(Request.ACTION_LOGIN, data));

        if (response.isSuccess()) {
            return (User) response.getData();
        }
        throw new RuntimeException(response.getMessage());
    }

    /**
     * Đăng kí một tài khoản mới
     *
     * @param username    tên người dùng
     * @param password    mật khẩu
     * @param fullName    họ tên
     * @param email       email
     * @param phoneNumber số điện thoại
     * @param gender      giới tính
     * @param role        vai trò : Bidder hoặc Seller
     */
    public void register(String username, String password, String fullName,
                         String email, String phoneNumber, String gender, String role) {
        Map<String, String> data = new HashMap<>();

        data.put("username", username);
        data.put("password", password);
        data.put("fullName", fullName);
        data.put("email", email);
        data.put("phoneNumber", phoneNumber);
        data.put("gender", gender);
        data.put("role", role);

        Response response = sendRequest(new Request(Request.ACTION_REGISTER, data));

        if (!response.isSuccess()) {
            throw new RuntimeException(response.getMessage());
        }
    }

    /**
     * Lấy danh sách các phiên đấu giá
     *
     * @return trả về danh sách các phiên đấu giá
     */
    @SuppressWarnings("unchecked")
    public List<Auction> getAllAuctions() {
        Response response = sendRequest(new Request(Request.ACTION_GET_AUCTIONS, null));
        if (response.isSuccess()) {
            return (List<Auction>) response.getData();
        }
        throw new RuntimeException(response.getMessage());
    }

    /**
     * Lấy danh sách phiên của seller
     * @param sellerId id của seller
     * @return danh sách phiên của seller
     */
    @SuppressWarnings("unchecked")
    public List<Auction> getAuctionsBySeller(String sellerId){
        Response response = sendRequest(new Request(Request.ACTION_GET_AUCTIONS_BY_SELLER, sellerId));
        if(response.isSuccess()){
            return (List<Auction>) response.getData();
        }
        throw new RuntimeException(response.getMessage());

    }

    /**
     * Lấy danh sách item của seller
     * @param sellerId id của seller
     * @return danh sách item của seller
     */
    public List<Item> getItemsBySeller(String sellerId){
        Response response = sendRequest(new Request(Request.ACTION_GET_ITEMS_BY_SELLER, sellerId));
        if(response.isSuccess()){
            return (List<Item>) response.getData();
        }
        throw new RuntimeException(response.getMessage());

    }

    /**
     * Hủy phiên đấu giá
     * @param auctionId id của phiên cần hủy
     * @return phiên đã hủy
     */
    public Auction cancelAuction(String auctionId){
        Response response = sendRequest(new Request(Request.ACTION_CANCEL_AUCTION, auctionId));
        if(response.isSuccess()){
            return (Auction) response.getData();
        }
        throw new RuntimeException(response.getMessage());
    }

    /**
     * Lấy tất cả danh sách user
     * @return danh sách user
     */
    public List<User> getAllUsers(){
        Response response = sendRequest(new Request(Request.ACTION_GET_ALL_USERS, null));
        if (response.isSuccess()) {
            return (List<User>) response.getData();
        }
        throw new RuntimeException(response.getMessage());
    }

    /**
     * admin lock user
     * @param user người bị lock
     * @return người bị lock
     */
    public User toggleUserLock(User user){
        Response response = sendRequest(new Request(Request.ACTION_TOGGLE_USER_LOCK, user));
        if (response.isSuccess()) {
            return (User) response.getData();
        }
        throw new RuntimeException(response.getMessage());
    }

    /**
     * Lấy thông tin 1 phiên đấu giá theo ID
     *
     * @param auctionId ID của phiên đấu giá
     * @return Auction hoặc null nếu không tìm thấy
     */
    public Auction getAuctionById(String auctionId) {
        Response response = sendRequest(new Request(Request.ACTION_GET_AUCTION_BY_ID, auctionId));

        if (response.isSuccess()) {
            return (Auction) response.getData();
        }
        return null;
    }

    /**
     * Tạo sản phẩm và tạo luôn phiên đấu giá
     * @param item sản phẩm cần tạo
     * @return phiên đấu giá đã tạo
     */
    public Auction createItemAndAuction(Item item){
        Response response = sendRequest(new Request(Request.ACTION_CREATE_AUCTION_ITEM, item));
        if(response.isSuccess()){
            return (Auction) response.getData();
        }
        throw new RuntimeException(response.getMessage());
    }

    /**
     * Bật chế độ auto bid
     * @param auctionId id của phiên đấu giá
     * @param bidderId id của bidder
     * @param maxBix số tiền mà bidder đưa để tự động dấu giá
     * @param increment bước giá
     */
    public void enableAutoBid(String auctionId, String bidderId, double maxBix, double increment){
        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId);
        data.put("bidderId", bidderId);
        data.put("maxBid", maxBix);
        data.put("increment", increment);
        sendRequest(new Request(Request.ACTION_ENABLE_AUTO_BID, data));
    }

    /**
     * tắt chế độ auto bid
     * @param auctionId id của phiên đấu giá
     * @param bidderId id của bidder
     */
    public void disableAutoBid(String auctionId, String bidderId){
        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId);
        data.put("bidderId", bidderId);
        sendRequest(new Request(Request.ACTION_DISABLE_AUTO_BID, data));
    }

    /**
     * Đặt giá cho một phiên đấu giá
     *
     * @param auctionId ID phiên đấu giá
     * @param bidderId  ID người đặt giá
     * @param amount    số tiền đặt giá
     * @return
     */
    public Auction placeBid(String auctionId, String bidderId, double amount) {
        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId);
        data.put("bidderId", bidderId);
        data.put("amount", amount);
        Response response = sendRequest(new Request(Request.ACTION_PLACE_BID, data));

        if (response.isSuccess()) {
            return (Auction) response.getData();
        }
        throw new RuntimeException(response.getMessage());
    }

    public Auction forceEndAuction(String auctionId){
        Response response = sendRequest(new Request(Request.ACTION_FORCE_END_AUCTION, auctionId));
        if(response.isSuccess()){
            return (Auction) response.getData();
        }
        throw new RuntimeException(response.getMessage());
    }

    /**
     * Lấy lịch sử bid của một phiên
     *
     * @param auctionId ID của một phiên đấu giá
     * @return danh sách giao dịch bid
     */
    public List<BidTransaction> getBidHistory(String auctionId) {
        Response response = sendRequest(new Request(Request.ACTION_GET_BID_HISTORY, auctionId));
        if (response.isSuccess()) {
            return (List<BidTransaction>) response.getData();
        }
        throw new RuntimeException(response.getMessage());
    }

    public List<BidTransaction> getBidsByAuction(String auctionId){
        Response response = sendRequest(new Request(Request.ACTION_GET_BIDS_BY_AUCTION, auctionId));
        if(response.isSuccess()){
            return (List<BidTransaction>) response.getData();
        }
        throw new RuntimeException(response.getMessage());
    }

    /**
     * Gửi Request lên Servẻ và đợi Response
     *
     * @param request yêu cầu cần gửi
     * @return phản hồi từ server
     */
    private synchronized Response sendRequest(Request request) {
        ConnectionManager connectionManager = ConnectionManager.getInstance();
        if (!connectionManager.isConnected()) {
            logger.warn("Connection lost. Attempting to reconnect");
            if (!connectionManager.reconnect()) {
                throw new RuntimeException("Cannot connect to server. Please try again");
            }
        }
        try{
            connectionManager.getOutputStream().writeObject(request);
            connectionManager.getOutputStream().flush();
            connectionManager.getOutputStream().reset();

            Response response = (Response) MessageRouter.getInstance().takeResponse();
            logger.info("Response: " + response);
            return response;
        } catch (IOException e){
            logger.warn("IO error during request: {}", request.getAction(), e);
            connectionManager.disconnect();
            throw new RuntimeException("Connection error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted");
        }
    }
    /**
     * Báo server biết client đang xem phiên này -> nhận thông báo
     * gọi khi mở màn BiddingController
     * @param auctionId id của phiên đấu giá
     */
    public void watchAuction(String auctionId){
        sendRequest(new Request(Request.ACTION_WATCH_AUCTION, auctionId));
    }

    /**
     * Báo server biết client thoát phiên này -> không nhận thông báo nữa
     * @param auctionId id của phiên đấu giá
     */
    public void leaveAuction(String auctionId){
        sendRequest(new Request(Request.ACTION_LEAVE_AUCTION, auctionId));
    }
}
