package org.example.loginregister.client.service;

import org.example.loginregister.server.common.network.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.BlockingQueue;
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
    private final BlockingQueue<Object> responseQueue = new LinkedBlockingQueue<>();

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
                }
                else {
                    responseQueue.put(obj);
                }
            } catch (IOException e){
                logger.warn("Connection lost in MessageRouter", e);
            } catch (ClassNotFoundException | InterruptedException e) {
                logger.warn("Error in MessageRouter: {}", e.getMessage());
            }
        }
    }

    public Object takeResponse() throws InterruptedException {
        return responseQueue.take();
    }

}
