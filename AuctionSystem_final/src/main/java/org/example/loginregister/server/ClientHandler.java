package org.example.loginregister.server;

import org.example.loginregister.server.model.entity.user.User;
import org.example.loginregister.server.network.Request;
import org.example.loginregister.server.network.Response;
import org.example.loginregister.server.util.AuctionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
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
     * Gửi phản hồi cho Client
     * @param response phản hồi
     */
    private void sendResponse(Response response) {
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
    }

    /**
     * Xử lý các yêu cầu từ Client
     * @param request yêu cầu từ Client
     * @return
     */
    private Response handleRequest(Request request){
        Function<Request, Response> handler = handlers.get(request.getAction());
        if(handler == null){
            return Response.error("Unknow action: " + request.getAction());
        }
        return handler.apply(request);
    }

    private Response handleLogin(Request request) {
        try {

            Map<String, String> credentials = (Map<String, String>) request.getData();

            String userName = credentials.get("username");
            String password = credentials.get("password");

            if (userName == null || password == null) {
                return Response.error("Username and password are required.");
            }

            User user = AuctionManager.getInstance().
        }
    }


}
