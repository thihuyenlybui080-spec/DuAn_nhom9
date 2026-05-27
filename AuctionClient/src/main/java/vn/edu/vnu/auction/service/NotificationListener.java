package vn.edu.vnu.auction.service;

import vn.edu.vnu.auction.common.network.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final Map<Integer, Consumer<NotificationMessage>> handlers = new ConcurrentHashMap<>();

    /**
     * khi client vào một auction thì sẽ đăng kí, gọi handler tương ứng
     * @param auctionId id của phiên đấu giá
     * @param handler handler
     */
    public void register(int auctionId, Consumer<NotificationMessage> handler){
        handlers.put(auctionId, handler);
        logger.info("Handler registered for auction: {}", auctionId);
    }

    /**
     * Khi client thoát phiên thì hủy đăng kí
     * @param auctionId id của phiên đấu giá
     */
    public void unregister(int auctionId){
        handlers.remove(auctionId);
        logger.info("Handler unregistered for auction: {}", auctionId);
    }

    /**
     * Chuyển hướng nhận notification và tìm handler tương ứng
     * @param notification thông báo từ NotificationMessage
     */
    public void dispatch(NotificationMessage notification) {
        Consumer<NotificationMessage> handler = handlers.get(notification.getAuctionId());
        if (handler != null) {
            handler.accept(notification);
        } else {
            logger.info("No handler for auction: {}", notification.getAuctionId());
        }
    }


}
