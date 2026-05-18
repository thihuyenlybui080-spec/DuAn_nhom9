package org.example.loginregister.client.service;

import org.example.loginregister.server.common.network.NotificationMessage;
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
public class NotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(NotificationListener.class);
    private static NotificationListener instance;
    public static NotificationListener getInstance(){
        if(instance == null){
            instance = new NotificationListener();
        }
        return instance;
    }
    private final Map<String, Consumer<NotificationMessage>> handlers = new ConcurrentHashMap<>();

    public void register(String auctionId, Consumer<NotificationMessage> handler){
        handlers.put(auctionId, handler);
        logger.info("Handler registered for auction: {}", auctionId);
    }

    public void unregister(String auctionId){
        handlers.remove(auctionId);
        logger.info("Handler unregistered for auction: {}", auctionId);
    }

    public void dispatch(NotificationMessage notification){
        Consumer<NotificationMessage> handler = handlers.get(notification.getAuctionId());
        if(handler != null){
            handler.accept(notification);
        } else {
            logger.info("No handler for auction: {}", notification.getAuctionId());
        }
    }



}
