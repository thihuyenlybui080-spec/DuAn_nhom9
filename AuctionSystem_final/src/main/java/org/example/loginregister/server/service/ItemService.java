package org.example.loginregister.server.service;

import org.example.loginregister.server.dao.AuctionDAO;
import org.example.loginregister.server.dao.ItemDAO;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.Seller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Dịch vụ truy vấn và quản lý sản phẩm (item) theo seller.
 */
public class ItemService {

    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);
    private static volatile ItemService instance;

    private ItemService() {}

    /**
     * @return singleton {@link ItemService}
     */
    public static ItemService getInstance() {
        if (instance == null) {
            synchronized (ItemService.class) {
                if (instance == null) {
                    instance = new ItemService();
                }
            }
        }
        return instance;
    }

    /**
     * Lấy danh sách item của seller từ DB.
     *
     * @param sellerId id seller (prefix hoặc số nguyên)
     * @return danh sách item, rỗng nếu id không hợp lệ
     */
    public List<Item> getItemsBySeller(String sellerId) {
        if (sellerId == null) {
            return Collections.emptyList();
        }
        int dbId = AuctionDAO.parseDbId(sellerId);
        if (dbId < 0) {
            try {
                dbId = Integer.parseInt(sellerId);
            } catch (NumberFormatException e) {
                logger.warn("getItemsBySeller: invalid sellerId {}", sellerId);
                return Collections.emptyList();
            }
        }
        List<Item> items = ItemDAO.getItemsBySeller(dbId);
        logger.debug("Found {} items for seller {}", items.size(), sellerId);
        return items;
    }
    public void deleteItem(String itemId){
        int dbId = ItemDAO.parseDbId(itemId);
        ItemDAO.deleteItem(dbId);
    }

    public static synchronized void resetForTesting() {
        instance = null;
    }
}
