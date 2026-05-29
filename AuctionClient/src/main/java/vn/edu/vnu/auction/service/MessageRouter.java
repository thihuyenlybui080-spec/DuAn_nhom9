package vn.edu.vnu.auction.service;

import vn.edu.vnu.auction.common.network.NotificationMessage;
import vn.edu.vnu.auction.common.network.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Trung tâm phân loại thông báo
 * <p> luồng hoạt động
 *     <pre>
 *         nhận Thông báo
 *         - thông báo thuộc NotificationMessage thì chuyển cho NotificationListener
 *         - thông báo còn lại để AuctionClientService xử lý
 *     </pre>
 * </p>
 */
public class MessageRouter implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(MessageRouter.class);
    private static MessageRouter instance;

    public static MessageRouter getInstance(){
        if(instance == null){
            instance = new MessageRouter();
        }
        return instance;
    }
    private final ConcurrentHashMap<String, LinkedBlockingQueue<Response>> pendingRequest = new ConcurrentHashMap<>();

    private volatile boolean running = false;
    private Thread routerThread;

    public void start(){
        if(running) return;
        running = true;
        routerThread = new Thread(this, "MessageRouter");
        routerThread.setDaemon(true);
        routerThread.start();
        logger.info("MessageRouter started");
    }

    public void stop(){
        running = false;
        if(routerThread != null){
            routerThread.interrupt();
        }
    }

    /**
     * Luồng hoạt động chính
     * - đọc stream rồi phân loại
     */
    @Override
    public void run(){
        ObjectInputStream inputStream = ConnectionManager.getInstance().getInputStream();
        while (running){
            try{
                Object obj = inputStream.readObject();

                if(obj instanceof NotificationMessage){
                    NotificationListener.getInstance().dispatch((NotificationMessage) obj);
                } else if (obj instanceof Response response) {
                    String id = response.getRequestId();
                    LinkedBlockingQueue<Response> queue = pendingRequest.get(id);
                    if(queue != null){
                        queue.put(response);
                    }
                    else {
                        logger.warn("No pending request found for id: {}", id);
                    }
                }
            } catch (IOException e){
                logger.warn("Connection lost in MessageRouter", e);
                break;
            } catch (ClassNotFoundException | InterruptedException e) {
                logger.warn("Error in MessageRouter: {}", e.getMessage());
            }
        }
    }

    public LinkedBlockingQueue<Response> registerRequest(String requestId){
        LinkedBlockingQueue<Response> queue = new LinkedBlockingQueue<>();
        pendingRequest.put(requestId, queue);
        return queue;
    }

    public void unregisterRequest(String requestId){
        pendingRequest.remove(requestId);
    }}


