package vn.edu.vnu.auction;

import vn.edu.vnu.auction.common.network.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry quản lý danh sách client dang xem phiên đấu giá nào
 *
 * <p>
 *     khi client bắt đầu xem phiên -> đăng kí vào registry.
 *     khi có bid mới -> server dùng registry để notify đúng client
 *     <pre>
 *         auctionId -> set của ObjectOutputStream (mỗi stream là một client)
 *         "AUC-001" → [stream_A, stream_B, stream_C]
 *     </pre>
 * </p>
 */
public class ClientRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ClientRegistry.class);
    private final Map<Integer, Set<ObjectOutputStream>> registry = new ConcurrentHashMap<>();

    private static ClientRegistry instance;

    public static ClientRegistry getInstance() {
        if (instance == null) {
            instance = new ClientRegistry();
        }
        return instance;
    }

    private ClientRegistry() {
    }

    /**
     * Client bắt đầu xem một phiên -> đăng kí stream vào registry
     * <p>
     * gọi từ ClientHandler khi nhận action WATCH_AUCTION.
     * </p>
     *
     * @param auctionId ID phiên đang xem
     * @param stream    ObjectOutputStream của client vừa đăng kí
     */
    public void register(int auctionId, ObjectOutputStream stream) {
        registry.computeIfAbsent(auctionId, k -> ConcurrentHashMap.newKeySet())
                .add(stream);
        logger.info("Client registered for auction: {} (total: {} )", auctionId, registry.get(auctionId).size());
    }

    /**
     * Client thoát khỏi phiên -> hủy đăng kí
     * <p>
     * gọi từ ClientHandler khi nhận action LEAVE_AUCTION
     * </p>
     *
     * @param auctionId ID của phiên
     * @param stream    ObjectOutputStream của người cần hủy
     */
    public void unregister(int auctionId, ObjectOutputStream stream) {
        Set<ObjectOutputStream> streams = registry.get(auctionId);
        if (streams != null) {
            streams.remove(stream);
            if (streams.isEmpty()) {
                registry.remove(auctionId);
            }
        }
        logger.info("Client unregistered from auction: {}", auctionId);
    }

    /**
     * khi client ngắt kết nối -> xóa stream khỏi tất cả các phiên
     *
     * @param stream ObjectOutputStream cần xóa
     */
    public void unregisterAll(ObjectOutputStream stream) {
        registry.values().forEach(set -> set.remove(stream));
        logger.info("Client stream remove from all aucions");
    }

    public void notifyAll(int auctionId, NotificationMessage notification) {
        Set<ObjectOutputStream> streams = registry.get(auctionId);
        if (streams == null || streams.isEmpty()) {
            return;
        }
        logger.info("Notifying {} client(s) for auction {}", streams.size(), auctionId);

        streams.removeIf(stream -> {
            try {
                synchronized (stream) {
                    stream.writeObject(notification);
                    stream.flush();
                    stream.reset();
                }
                return false;
            } catch (IOException e) {
                logger.warn("Failed to notify client, removing from registry.");
                return true;
            }
        });
    }
}
