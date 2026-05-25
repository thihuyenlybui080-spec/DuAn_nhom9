package org.example.loginregister.server;

import org.example.loginregister.server.common.exception.AuctionClosedException;
import org.example.loginregister.server.common.exception.DuplicateUsernameException;
import org.example.loginregister.server.common.exception.InvalidBidException;
import org.example.loginregister.server.common.network.NotificationMessage;
import org.example.loginregister.server.dao.AuctionDAO;
import org.example.loginregister.server.dao.AutoBidDAO;
import org.example.loginregister.server.dao.UserDAO;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.BidTransaction;
import org.example.loginregister.server.model.entity.auto_bidding.AutoBidConfig;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.Admin;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.User;
import org.example.loginregister.server.model.entity.user.UserStatus;
import org.example.loginregister.server.common.network.Request;
import org.example.loginregister.server.common.network.Response;
import org.example.loginregister.server.service.*;
import org.example.loginregister.server.util.AuctionHistoryManager;
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
            logger.info("Client disconnected: " + e.getMessage());
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

            UserDAO.registerUser(username, password, fullName, email, gender, phoneNumber, role);

            return Response.ok("Registration successful.", null);
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
            String auctionId = (String) request.getData();
            Auction auction = AuctionService.getInstance().getAuction(auctionId);

            if(auctionId == null){
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

            String auctionId = (String) bidData.get("auctionId");
            String bidderId = (String) bidData.get("bidderId");
            double amount = ((Number) bidData.get("amount")).doubleValue();

            Auction auction = AuctionService.getInstance().getAuction(auctionId);

            if(auction == null){
                return Response.error("Auction not found");
            }

            loggedInUser = UserDAO.getUserById(Integer.parseInt(bidderId.split("-")[1]));

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
        String itemId = (String) request.getData();
        ItemService.getInstance().deleteItem(itemId);
        return Response.ok("Item deleted successfully.", null);
    }

    private Response handleCreateAuctionAndItem(Request request){
        try{
            Item item= (Item) request.getData();
            String sellerId = item.getSellerId();
            User seller = UserDAO.getUserById(AuctionDAO.parseDbId(sellerId));
            
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
            String auctionId = (String) request.getData();
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
            String auctionId = (String) request.getData();
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
            String bidderId = (String) request.getData();
            if(bidderId == null){
                return Response.error("BidderId not found");
            }
            if(!(loggedInUser instanceof Bidder)){
                return Response.error("Only Bidder can get won auctions");
            }
            Bidder bidder = (Bidder) loggedInUser;
            bidder.refreshWonAuctions();
            return Response.ok(bidder.getWonAuctions());
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
            String auctionId = (String) request.getData();
            if (auctionId == null) {
                return Response.error("AuctionId not found");}
            if (!(loggedInUser instanceof Bidder)) {
                return Response.error("Only Bidder can pay auction");}
            boolean success = PaymentService.getInstance().processPayment((Bidder) loggedInUser, auctionId);
            if (!success) {
                return Response.error("Payment failed");}
            return Response.ok("Payment successful", auctionId);
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
        String auctionId = (String) request.getData();
        ClientRegistry.getInstance().register(auctionId, outputStream);
        return Response.ok("Watching auction: " + auctionId, null);

    }

    /**
     * Thoát khỏi phiên đấu giá
     * @param request yêu cầu từ khách hàng
     * @return phản hồi từ server
     */
    private Response handleLeaveAuction(Request request){
        String auctionId = (String) request.getData();
        ClientRegistry.getInstance().unregister(auctionId, outputStream);
        return Response.ok("Left auction: " + auctionId, null);
    }

    private Response handleGetAuctionsBySeller(Request request){
        try {
            String sellerId = (String) request.getData();
            if (sellerId == null) {
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
            String auctionId = (String) request.getData();
            if(auctionId == null){
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
            String auctionId = (String) request.getData();
            if(auctionId == null){
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
            String sellerId = (String) request.getData();
            if (sellerId == null) {
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
            String auctionId = (String) data.get("auctionId");
            String bidderId = (String) data.get("bidderId");
            double maxBid = ((Number) data.get("maxBid")).doubleValue();
            double increment = ((Number) data.get("increment")).doubleValue();

            logger.info("handleEnableAutoBid: auctionId={}, bidderId={}, maxBid={}, increment={}", auctionId, bidderId, maxBid, increment);

            Auction auction = AuctionService.getInstance().getAuction(auctionId);
            if (auction == null){
                logger.warn("Auction not found: {}", auctionId);
                return Response.error("Auction not found");
            }

            // Check if auction has expired
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
            logger.info("User object hash: {}", System.identityHashCode(loggedInUser));
            ((Bidder) loggedInUser).enableAutoBid(auction, new AutoBidConfig(maxBid, increment));
            logger.info("enableAutoBid call completed");
            return Response.ok("Auto-bid enabled", null);
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
            String auctionId = (String) data.get("auctionId");
            String bidderId = (String) data.get("bidderId");
            loggedInUser = UserService.getInstance().getUserById(bidderId);
            if(loggedInUser == null){
                return Response.error("Bidder not found");
            }
            if(!(loggedInUser instanceof Bidder)){
                return Response.error("Only bidders can use auto bid");
            }
            ((Bidder) loggedInUser).disableAutoBid(auctionId);
            logger.info("AutoBid disabled: user={} auction={}", loggedInUser.getFullName(), auctionId);

            return Response.ok("Auto bid disabled", null);
        } catch (Exception e){
            logger.warn("DisableAutoBid error: {}", e.getMessage());
            return Response.error("Failed to disable auto-bid: " + e.getMessage());
        }
    }

    private Response handleCheckAutoBid(Request request){
        try{
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) request.getData();
            String auctionId = (String) data.get("auctionId");
            String bidderId = (String) data.get("bidderId");

            int auctionDbId = AuctionDAO.parseDbId(auctionId);
            int bidderDbId = AuctionDAO.parseDbId(bidderId);

            if (auctionDbId <= 0 || bidderDbId <= 0) {
                return Response.error("Invalid IDs");
            }

            AutoBidConfig config = AutoBidDAO.getAutoBidConfig(auctionDbId, bidderDbId);
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
            String bidderId = (String) request.getData();
            int bidderDbId = AuctionDAO.parseDbId(bidderId);

            if (bidderDbId <= 0) {
                return Response.error("Invalid bidder ID");
            }

            List<org.example.loginregister.server.model.entity.BidTransaction> history =
                    org.example.loginregister.server.dao.BidDAO.getBidHistory(bidderDbId, null);

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
            outputStream.writeObject(response);
            outputStream.flush();
            outputStream.reset();
        } catch (IOException e){
            logger.error("Failed to send response:", e);
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
        User freshUser = UserDAO.getUserById(Integer.parseInt(user.getId().split("-")[1]));
        return freshUser != null && freshUser.isActive();
    }

}

