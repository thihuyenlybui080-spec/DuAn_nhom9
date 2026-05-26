package vn.edu.vnu.auction.service;

import vn.edu.vnu.auction.dao.AuctionDAO;
import vn.edu.vnu.auction.dao.ItemDAO;
import vn.edu.vnu.auction.model.entity.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @param sellerId id seller (số nguyên)
     * @return danh sách item, rỗng nếu id không hợp lệ
     */
    public List<Item> getItemsBySeller(int sellerId) {
        if (sellerId < 0) {
            return Collections.emptyList();
        }
        List<Item> items = ItemDAO.getItemsBySeller(sellerId);
        logger.debug("Found {} items for seller {}", items.size(), sellerId);
        return items;
    }

    public void deleteItem(int itemId){
        ItemDAO.deleteItem(itemId);
    }

    public static synchronized void resetForTesting() {
        instance = null;
    }
}
