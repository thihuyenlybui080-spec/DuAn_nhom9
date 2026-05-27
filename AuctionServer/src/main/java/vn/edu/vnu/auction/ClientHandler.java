package vn.edu.vnu.auction;

import vn.edu.vnu.auction.common.exception.AuctionClosedException;
import vn.edu.vnu.auction.common.exception.DuplicateUsernameException;
import vn.edu.vnu.auction.common.exception.InvalidBidException;
import vn.edu.vnu.auction.common.network.NotificationMessage;
import vn.edu.vnu.auction.dao.AuctionDAO;
import vn.edu.vnu.auction.dao.AutoBidDAO;
import vn.edu.vnu.auction.dao.UserDAO;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.AuctionResult;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.BidTransaction;
import vn.edu.vnu.auction.model.entity.auto_bidding.AutoBidAgent;
import vn.edu.vnu.auction.model.entity.auto_bidding.AutoBidConfig;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.Admin;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.model.entity.user.User;
import vn.edu.vnu.auction.common.network.Request;
import vn.edu.vnu.auction.common.network.Response;
import vn.edu.vnu.auction.service.*;
import vn.edu.vnu.auction.util.AuctionHistoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Xử lý 1 client trên 1 thread riêng.
 *
 * <p>Luồng hoạt động:
 * <pre>
 *     Client kết nôt -> Server tạo ClientHandler mới -> chạy trong thread riêng
 *     - đọc yêu cầu từ client
 *     - gọi handleRequest() nhận yêu cầu
 *     -gửi phản hồi về client
 *     -lặp lại cho đến khi client ngắt kết nối
 * </pre>
 * </p>
 */
public class ClientHandler implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket clientSocket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private User loggedInUser;

    private Map<String, Function<Request, Response>> handlers = new HashMap<>();

    /**
     *Tạo một socket kết nối vơi client
     * @param clientSocket kết nối với client
     */
    public ClientHandler(Socket clientSocket){
        this.clientSocket = clientSocket;
        initHandlers();
    }

    /**
     * chu trình: đọc Request -> xử lý -> Gửi Response.
     * chạy liên tục đến khi Client ngắt kết nối
     */
    @Override
    public void run(){
        try {
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(clientSocket.getInputStream());
            logger.info("Client connected: " + clientSocket.getInetAddress());

            while(!clientSocket.isClosed()) {
                try {
                    Request request = (Request) inputStream.readObject();
                    logger.info("Received request: " + request);
                    Response response = handleRequest(request);

                    sendResponse(response);
                } catch (ClassNotFoundException e) {
                    logger.warn("Unknow Object receive: " + e.getMessage());
                    sendResponse(Response.error("Invalid request format"));
                }

            }
        } catch (IOException e) {
            logger.warn("Client disconnected - IOException: {} (socket closed: {}, connected: {})",
                    e.getMessage(), clientSocket.isClosed(), clientSocket.isConnected());
        }
        finally {
            cleanup();
        }
    }
    /**
     * Khởi tạo các trình xử lý sự kiện
     */
    private void initHandlers(){
        handlers.put(Request.ACTION_LOGIN, this :: handleLogin);
        handlers.put(Request.ACTION_REGISTER, this :: handleRegister);
        handlers.put(Request.ACTION_GET_AUCTIONS, this :: handleGetAuctions);
        handlers.put(Request.ACTION_GET_AUCTION_BY_ID, this :: handleGetAuctionById);
        handlers.put(Request.ACTION_PLACE_BID, this :: handlePlaceBid);
        handlers.put(Request.ACTION_GET_BID_HISTORY, this :: handleGetBidHistory);
        handlers.put(Request.ACTION_WATCH_AUCTION, this :: handleWatchAuction);
        handlers.put(Request.ACTION_LEAVE_AUCTION, this :: handleLeaveAuction);
        handlers.put(Request.ACTION_GET_AUCTIONS_BY_SELLER, this::handleGetAuctionsBySeller);
        handlers.put(Request.ACTION_CANCEL_AUCTION, this :: handleCancelAuction);
        handlers.put(Request.ACTION_FORCE_END_AUCTION, this :: handleForceEndAuction);
        handlers.put(Request.ACTION_GET_ALL_USERS, this :: handleGetAllUsers);
        handlers.put(Request.ACTION_TOGGLE_USER_LOCK, this :: handleToggleUserLock);
        handlers.put(Request.ACTION_GET_ITEMS_BY_SELLER, this :: handleGetItemsBySeller);
        handlers.put(Request.ACTION_CREATE_AUCTION_ITEM, this :: handleCreateAuctionAndItem);
        handlers.put(Request.ACTION_GET_BIDS_BY_AUCTION, this :: handleGetBidsByAuction);
        handlers.put(Request.ACTION_DISABLE_AUTO_BID, this :: handleDisableAutoBid);
        handlers.put(Request.ACTION_ENABLE_AUTO_BID, this :: handleEnableAutoBid);
        handlers.put(Request.ACTION_CHECK_AUTO_BID, this :: handleCheckAutoBid);
        handlers.put(Request.ACTION_GET_BIDDER_HISTORY, this :: handleGetBidderHistory);
        handlers.put(Request.ACTION_DELETE_ITEM, this :: handleDeleteItem);
        handlers.put(Request.ACTION_GET_WON_AUCTIONS, this :: handelGetWonAuctions);
        handlers.put(Request.ACTION_PAY_AUCTION, this :: handlePayAuction);
    }

    /**
     * Xử lý các yêu cầu từ Client
     * @param request yêu cầu từ Client
     * @return
     */
    private Response handleRequest(Request request){
        Function<Request, Response> handler = handlers.get(request.getAction());
        if(handler == null){
            return Response.error("Unknow action: {} " + request.getAction());
        }
        Response response = handler.apply(request);
        response.setRequestId(request.getRequestId());
        return response;
    }

    private Response handleLogin(Request request) {
        try {

            Map<String, String> credentials = (Map<String, String>) request.getData();

            String userName = credentials.get("username");
            String password = credentials.get("password");

            if (userName == null || password == null) {
                return Response.error("Username and password are required.");
            }

            User user = UserDAO.getUserByCredentials(userName, password);

            if(user == null){
                return Response.error("Invalid username or password");
            }

            if(!user.isActive()){
                return Response.error("This account has been locked");
            }

            this.loggedInUser = user;
            logger.info("User logged in: {}", userName);

            return Response.ok("Login successful.", user);
        } catch (Exception e){
            logger.warn("Login error: {}", e.getMessage());
            return Response.error("Login failed: " + e.getMessage());
        }
    }

    /**
     * Xử lý đăng kí từ Client
     * @param request yêu cầu từ client
     * @return trả về phản hồi
     */
    private Response handleRegister(Request request){
        try{
            @SuppressWarnings("unchecked")
            Map<String, String> info = (Map<String, String>) request.getData();

            String username = info.get("username");
            String password = info.get("password");
            String role = info.get("role");
            String fullName = info.get("fullName");
            String gender = info.get("gender");
            String phoneNumber = info.get("phoneNumber");
            String email = info.get("email");

            if(username == null || password == null || role == null){
                return Response.error("Missing required fields");
            }

            User user = UserDAO.registerUser(username, password, fullName, email, gender, phoneNumber, role);

            Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("userId", user.getId());
            responseData.put("message", "Registration successful");

            return Response.ok(responseData);
        } catch (DuplicateUsernameException e){
            logger.warn("Register error: {}", e.getMessage());
            return Response.error("Username already exists");
        } catch (Exception e){
            logger.warn("Register error: {}", e.getMessage());
            return Response.error("Registration failed: " + e.getMessage());
        }
    }

    /**
     * lấy danh sách tất cả phiên đấu giá
     * @param request yêu cầu từ client
     * @return một phản hồi
     */
    private Response handleGetAuctions(Request request){
        try{
            logger.info("handleGetAuctions: Retrieving all auctions");
            List<Auction> auctions = AuctionService.getInstance().getAllAuctions();
            logger.info("handleGetAuctions: Retrieved {} auctions", auctions != null ? auctions.size() : 0);
            return Response.ok(auctions);
        } catch (Exception e){
            logger.error("GetAuctions error: ", e);
            return Response.error("Failed to get auctions: " + e.getMessage());
        }
    }


    private Response handleGetAuctionById(Request request){
        try{
            int auctionId = (Integer) request.getData();
            Auction auction = AuctionService.getInstance().getAuction(auctionId);

            if(auction == null){
                return Response.error("Auction not found: " + auctionId);
            }

            return Response.ok(auction);
        } catch (Exception e){
            logger.warn("GetAuctionById error: {} ", e.getMessage());
            return Response.error("Failed to get auctions");
        }
    }

    /**
     * Xử lý đặt giá
     * @param request yêu cầu từ client
     * @return phản hồi từ server
     */
    private Response handlePlaceBid(Request request) {
        try{
            @SuppressWarnings("unchecked")
            Map<String, Object> bidData = (Map<String, Object>) request.getData();

            int auctionId = (Integer) bidData.get("auctionId");
            int bidderId = (Integer) bidData.get("bidderId");
            double amount = ((Number) bidData.get("amount")).doubleValue();

            Auction auction = AuctionService.getInstance().getAuction(auctionId);

            if(auction == null){
                return Response.error("Auction not found");
            }

            loggedInUser = UserDAO.getUserById(bidderId);

            if(loggedInUser == null){
                return Response.error("Bidder not found");
            }

            if(!(loggedInUser instanceof Bidder)){
                return Response.error("only bidders can place bids. Current user type: " + loggedInUser.getClass().getSimpleName());
            }

            if(!isUserActive(loggedInUser)){
                return Response.error("Your account has been locked. Please contact admin.");
            }

            logger.info("PlaceBid attempt - Bidder: {}, Bidder class: {}",
                    loggedInUser.getName(),
                    loggedInUser.getClass().getSimpleName());

            LocalDateTime endTimeBefore = auction.getItem().getEndTime();
            BidService.getInstance().placeBid(auctionId, (Bidder) loggedInUser, amount);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            auction = AuctionService.getInstance().getAuction(auctionId);
            if (auction.getBids() != null && !auction.getBids().isEmpty()) {
                double highestBidAmount = auction.getBids().stream()
                        .mapToDouble(BidTransaction::getAmount)
                        .max()
                        .orElse(auction.getCurrentPrice());
                auction.setCurrentPrice(highestBidAmount);
            }

            ClientRegistry.getInstance().notifyAll(auctionId, new NotificationMessage(
                    NotificationMessage.TYPE_BID_UPDATED,
                    auctionId,
                    auction
            ));
            if(!auction.getItem().getEndTime().equals(endTimeBefore)){
                ClientRegistry.getInstance().notifyAll(auctionId, new NotificationMessage(
                        NotificationMessage.TYPE_TIME_EXTENDED,
                        auctionId,
                        auction
                ));
                logger.info("Anti_snipe broadcast: auction {} extended to {}", auctionId, auction.getItem().getEndTime());
            }
            return Response.ok("Bid placed successfully.", auction);
        }catch (InvalidBidException e){
            return Response.error(e.getMessage());
        } catch (AuctionClosedException e){
            return Response.error("Auction is closed");
        } catch (Exception e){
            logger.warn("PlaceBid error: {}", e.getMessage());
            return Response.error("Failed to place bid: " + e.getMessage());
        }
    }

    private Response handleDeleteItem(Request request){
        int itemId = (Integer) request.getData();
        ItemService.getInstance().deleteItem(itemId);
        return Response.ok("Item deleted successfully.", (Object) null);
    }

    private Response handleCreateAuctionAndItem(Request request){
        try{
            Item item= (Item) request.getData();
            int sellerId = item.getSellerId();
            User seller = UserDAO.getUserById(sellerId);
            
            if(seller == null){
                return Response.error("Seller not found");
            }
            
            if(!isUserActive(seller)){
                return Response.error("Your account has been locked. Please contact admin.");
            }
            
            Auction auction = AuctionService.getInstance().startAuction(item);
            return Response.ok(auction);
        }catch (Exception e){
            logger.warn("CreateAuctionAndItem error", e);
            return Response.error("Failed to create auction: " + e.getMessage());
        }
    }

    private Response handleGetBidsByAuction(Request request){
        try{
            int auctionId = (Integer) request.getData();
            List<BidTransaction> list = BidService.getInstance().getBidsByAuction(auctionId);
            return Response.ok(list);
        } catch (Exception e){
            logger.warn("GetBidsByAuction error", e);
            return Response.error("Failed to get bids by auction");
        }
    }

    /**
     * Lấy lịch sử bid của một phiên
     * @param request yêu cầu từ khách hàng
     * @return một phản hồi từ server
     */
    private Response handleGetBidHistory(Request request){
        try{
            int auctionId = (Integer) request.getData();
            return Response.ok(AuctionHistoryManager.getInstance().getResult(auctionId));
        } catch (Exception e){
            return Response.error("Failed to get bid history");
        }
    }

    /**
     * Lấy các auctions đã thắng
     * @param request yêu cầu từ client
     * @return phản hồi từ server
     */
    private Response handelGetWonAuctions(Request request){
        try{
            int bidderId = (Integer) request.getData();
            if(bidderId <= 0){
                return Response.error("BidderId not found");
            }
            List<AuctionResult> wonAuctions = AuctionService.getInstance().getWonAuctions(bidderId);
            return Response.ok(wonAuctions);
        } catch (Exception e){
            logger.warn("GetWonAuctions error", e);
            return Response.error("Failed to get won auctions");
        }
    }

    /**
     * Xử lý thanh toán cho auction
     * @param request yêu cầu thanh toán
     * @return phản hồi từ server
     */
    private Response handlePayAuction(Request request) {
        try {
            int auctionId = (Integer) request.getData();
            if (auctionId < 0) {
                return Response.error("AuctionId not found");}
            if (!(loggedInUser instanceof Bidder)) {
                return Response.error("Only Bidder can pay auction");}
            boolean success = PaymentService.getInstance().processPayment((Bidder) loggedInUser, auctionId);
            if (!success) {
                return Response.error("Payment failed");}
            return Response.ok("Payment successful", (Object) auctionId);
        } catch (Exception e) {
            logger.warn("PayAuction error", e);
            return Response.error("Failed to process payment");
        }
    }

    /**
     * Tham gia vào phiên đấu giá
     * @param request yêu cầu từ khách hàng
     * @return phản hồi từ server
     */
    private Response handleWatchAuction(Request request){
        int auctionId = (Integer) request.getData();
        ClientRegistry.getInstance().register(auctionId, outputStream);
        return Response.ok("Watching auction: " + auctionId, (Object) null);

    }

    /**
     * Thoát khỏi phiên đấu giá
     * @param request yêu cầu từ khách hàng
     * @return phản hồi từ server
     */
    private Response handleLeaveAuction(Request request){
        int auctionId = (Integer) request.getData();
        ClientRegistry.getInstance().unregister(auctionId, outputStream);
        return Response.ok("Left auction: " + auctionId, (Object) null);
    }

    private Response handleGetAuctionsBySeller(Request request){
        try {
            int sellerId = (Integer) request.getData();
            if (sellerId <= 0) {
                return Response.error("SellerID not found");
            }
            List<Auction> auctions = AuctionService.getInstance().getAuctionsBySeller(sellerId);
            return Response.ok(auctions);
        } catch (Exception e){
            logger.warn("GetAuctionsBySeller error", e);
            return Response.error("Failed to get auctions");
        }
    }

    private Response handleCancelAuction(Request request){
        try{
            int auctionId = (Integer) request.getData();
            if(auctionId <= 0){
                return Response.error("AuctionID not found");
            }
            Auction auction = AuctionService.getInstance().getAuction(auctionId);
            if(auction == null){
                return Response.error("Auction not found");
            }
            AuctionService.getInstance().cancelAuction(auctionId);
            ClientRegistry.getInstance().notifyAll(auctionId, new NotificationMessage(
                    NotificationMessage.TYPE_AUCTION_ENDED,
                    auctionId,
                    auction
            ));
            return Response.ok(auction);

        }catch (Exception e){
            logger.warn("Cancel auction error", e);
            return Response.error("Failed to cancel auction");
        }
    }

    private Response handleForceEndAuction(Request request){
        try{
            int auctionId = (Integer) request.getData();
            if(auctionId <= 0){
                return Response.error("AuctionID not found");
            }
            Auction auction = AuctionService.getInstance().endAuction(auctionId, true);
            return Response.ok(auction);
        }catch (Exception e){
            logger.warn("ForceEndAuction error", e);
            return Response.error("Failed to force end auction");
        }
    }



    private Response handleGetItemsBySeller(Request request){
        try {
            int sellerId = (Integer) request.getData();
            if (sellerId <= 0) {
                return Response.error("SellerID not found");
            }
            List<Item> items = ItemService.getInstance().getItemsBySeller(sellerId);
            return Response.ok(items);
        } catch (Exception e){
            logger.warn("GetItemsBySeller error", e);
            return Response.error("Failed to get items");
        }
    }

    private Response handleGetAllUsers(Request request){
        try{
            List<User> users = UserService.getInstance().getAllUsers();
            return Response.ok(users);
        } catch (Exception e){
            logger.warn("GetUsers error", e);
            return Response.error("Failed to get users");
        }
    }

    private Response handleToggleUserLock(Request request){
        try{
            User user = (User) request.getData();
            if(user == null){
                return Response.error("Invalid user");
            }
            if(user instanceof Admin){
                return Response.error("Admin can't lock another admin");
            }
            UserService.getInstance().toggleUserLock(user);
            return Response.ok(user);
        } catch (RuntimeException e){
            logger.error("handleToggleUserLock error", e);
            return  Response.error("Failed to lock user");
        }
    }

    private Response handleEnableAutoBid(Request request){
        try{
            Map<String, Object> data = (Map<String, Object>) request.getData();
            int auctionId = (Integer) data.get("auctionId");
            int bidderId = (Integer) data.get("bidderId");
            double maxBid = ((Number) data.get("maxBid")).doubleValue();
            double increment = ((Number) data.get("increment")).doubleValue();

            logger.info("handleEnableAutoBid: auctionId={}, bidderId={}, maxBid={}, increment={}", auctionId, bidderId, maxBid, increment);

            Auction auction = AuctionService.getInstance().getAuction(auctionId);
            if (auction == null){
                logger.warn("Auction not found: {}", auctionId);
                return Response.error("Auction not found");
            }
            if (auction.getStatus() == AuctionStatus.FINISHED) {
                logger.warn("Auction {} has finished, cannot enable auto-bid", auctionId);
                return Response.error("This auction has ended and cannot enable auto-bid.");
            }
            if (auction.getItem().getEndTime() != null && auction.getItem().getEndTime().isBefore(java.time.LocalDateTime.now())) {
                logger.warn("Auction {} has expired (endTime passed), cannot enable auto-bid", auctionId);
                return Response.error("This auction has expired and cannot enable auto-bid.");
            }

            loggedInUser = UserService.getInstance().getUserById(bidderId);
            if(loggedInUser == null){
                logger.warn("Bidder not found: {}", bidderId);
                return Response.error("Bidder not found");
            }

            if(!isUserActive(loggedInUser)){
                return Response.error("Your account has been locked. Please contact admin.");
            }
            if(!(loggedInUser instanceof Bidder)){
                logger.warn("User is not a bidder: {}", loggedInUser.getClass().getSimpleName());
                return Response.error("Only Bidders can use auto_bid");
            }

            logger.info("Calling enableAutoBid for bidder: {}, user object: {}", loggedInUser.getName(), loggedInUser.getClass().getName());
            AutoBidDAO.saveAutoBid(auctionId, bidderId, maxBid, increment);
            AutoBidConfig config = new AutoBidConfig(maxBid, increment);
            AutobidService.getInstance().enableAutoBid((Bidder) loggedInUser, auction, config);
            logger.info("enableAutoBid call completed");
            return Response.ok("Auto-bid enabled", (Object) null);
        } catch (Exception e){
            logger.error("EnableAutoBid error", e);
            e.printStackTrace();
            return Response.error("Failed to enable auto bid: " + e.getMessage());
        }
    }

    private Response handleDisableAutoBid(Request request){
        try{
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) request.getData();
            int bidderId = (Integer) data.get("bidderId");
            int auctionId = (Integer) data.get("auctionId");
            loggedInUser = UserService.getInstance().getUserById(bidderId);
            if(loggedInUser == null){
                return Response.error("Bidder not found");
            }
            if(!(loggedInUser instanceof Bidder)){
                return Response.error("Only bidders can use auto bid");
            }
            AutoBidDAO.deleteAutoBid(auctionId, bidderId);
            AutobidService.getInstance().disableAutoBid(auctionId, bidderId);
            logger.info("AutoBid disabled: user={}", loggedInUser.getFullName());

            return Response.ok("Auto bid disabled", (Object) null);
        } catch (Exception e){
            logger.warn("DisableAutoBid error: {}", e.getMessage());
            return Response.error("Failed to disable auto-bid: " + e.getMessage());
        }
    }

    private Response handleCheckAutoBid(Request request){
        try{
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) request.getData();
            int auctionId = (Integer) data.get("auctionId");
            int bidderId = (Integer) data.get("bidderId");

            if (auctionId <= 0 || bidderId <= 0) {
                return Response.error("Invalid IDs");
            }

            AutoBidConfig config = AutoBidDAO.getAutoBidConfig(auctionId, bidderId);
            boolean isActive = config != null;

            Map<String, Object> result = new HashMap<>();
            result.put("active", isActive);
            if (isActive) {
                result.put("maxBid", config.getMaxBid());
                result.put("increment", config.getIncrement());
            }

            return Response.ok("Auto-bid status checked", result);
        } catch (Exception e){
            logger.error("CheckAutoBid error", e);
            return Response.error("Failed to check auto-bid: " + e.getMessage());
        }
    }

    private Response handleGetBidderHistory(Request request){
        try{
            int bidderId = (Integer) request.getData();

            if (bidderId <= 0) {
                return Response.error("Invalid bidder ID");
            }

            List<vn.edu.vnu.auction.model.entity.BidTransaction> history =
                    vn.edu.vnu.auction.dao.BidDAO.getBidHistory(bidderId, null);

            return Response.ok("Bidder history retrieved", history);
        } catch (Exception e){
            logger.error("GetBidderHistory error", e);
            return Response.error("Failed to get bidder history: " + e.getMessage());
        }
    }

    /**
     * gửi phản hồi cho client
     * @param response phản hồi từ server
     * synchronized để tránh hai thread gửi cùng một lúc làm nhầm dữ liệu
     */
    private synchronized void sendResponse(Response response){
        try{
            logger.info("Sending response: {}", response);
            outputStream.writeObject(response);
            outputStream.flush();
            outputStream.reset();

            if (clientSocket.isClosed()) {
                logger.warn("Response sent but socket is closed - client may have disconnected");
            } else if (!clientSocket.isConnected()) {
                logger.warn("Response sent but socket is not connected - client may have disconnected");
            } else {
                logger.info("Response sent successfully, client still connected");
            }
        } catch (IOException e){
            logger.error("Failed to send response - client may have disconnected or network error: {}", e.getMessage());
        }
    }

    private void cleanup(){
        try{
            if(inputStream != null) inputStream.close();
            if(outputStream != null) outputStream.close();
            if(!clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            logger.warn("Cleanup error: " + e.getMessage());
        }
    }

    /**
     * Check if user is active (not locked/banned)
     */
    private boolean isUserActive(User user){
        if(user == null){
            return false;
        }
        User freshUser = UserDAO.getUserById(user.getId());
        return freshUser != null && freshUser.isActive();
    }

}

