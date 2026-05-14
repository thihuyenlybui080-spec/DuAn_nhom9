package org.example.loginregister.client.service;

import org.example.loginregister.common.network.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Lắng nghe thông báo từ Server
 * <p> Luồng hoạt động
 *     <pre>
 *         Server gửi tin nhắn thông báo (NotificationMessage) xuống
 *         - lắng nghe thông báo (NotificationListener) nhận được
 *         - tìm handler đã đăng kí cho auctionId đó
 *         - gọi handler -> handler dùng Platform.runlater() cập nhật màn UI
 *     </pre>
 * </p>
 */
public class NotificationListener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(NotificationListener.class)
    private final Map<String, Consumer<NotificationMessage>> handlers = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    private Thread ListenerThread;

    public void start(){
        if (running){
            return;
        }
        running = true;
        ListenerThread = new Thread(this, "NotificationListener");
        ListenerThread.setDaemon(true);
        ListenerThread.start();
        logger.info("NotificationListener started");
    }

    /**
     * Dừng Listener thread
     * gọi khi app tắt
     */
    public void stop(){
        running = false;
        if(ListenerThread != null){
            ListenerThread.interrupt();
        }
        logger.info("NotificationListener stopped");
    }

    /**
     * Vòng lặp chính: đọc thông báo từ Server liên tục
     */
    @Override
    public void run(){
        ObjectInputStream inputStream = ConnectionManager.getInstance().getInputStream();
        while(running){
            try{
                Object obj = inputStream.readObject();
                if(obj instanceof NotificationMessage){
                    NotificationMessage notification = (NotificationMessage) obj;
                    logger.info("Notification received: " + notification);
                    dispatch(notification);
                }
            } catch (IOException e){
                if(running){
                    logger.warn("Connection lost in NotificationListener.");
                    running = false;
                }
                break;
            } catch (ClassNotFoundException e){
                logger.warn("Unknow notification type: {}", e.getMessage());
            }
        }
    }
    



}
