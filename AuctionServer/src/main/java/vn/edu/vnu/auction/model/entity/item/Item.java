package vn.edu.vnu.auction.model.entity.item;

import vn.edu.vnu.auction.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Lớp cơ sở trừu tượng cho tất cả các sản phẩm đấu giá.
 * <p>
 * Đại diện cho một sản phẩm có thể đấu giá, bao gồm tên, mô tả,
 * giá khởi điểm, và khoảng thời gian đấu giá. Các lớp con định nghĩa các danh mục sản phẩm cụ thể.
 * </p>
 */
public abstract class Item extends Entity {
    private static final Logger logger = LoggerFactory.getLogger(Item.class);
    private String itemName;
    private final int createdBy;
    private String description;
    private double startingPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String imagePath;

    /**
     * Tạo một Item mới không có ID.
     *
     * @param itemName tên của sản phẩm
     * @param createdBy ID của người dùng đã tạo sản phẩm này
     * @param description mô tả sản phẩm
     * @param startingPrice giá khởi điểm
     * @param startTime thời gian bắt đầu đấu giá
     * @param endTime thời gian kết thúc đấu giá
     */
    public Item(String itemName, int createdBy, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime){
        super();
        this.createdBy = createdBy;
        this.itemName = itemName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Tạo một Item mới với ID cụ thể.
     *
     * @param id ID sản phẩm
     * @param itemName tên của sản phẩm
     * @param createdBy ID của người dùng đã tạo sản phẩm này
     * @param description mô tả sản phẩm
     * @param startingPrice giá khởi điểm
     * @param startTime thời gian bắt đầu đấu giá
     * @param endTime thời gian kết thúc đấu giá
     */
    public Item(int id, String itemName, int createdBy, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime){
        super(id);
        this.createdBy = createdBy;
        this.itemName = itemName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Lấy tên sản phẩm.
     *
     * @return tên sản phẩm
     */
    public String getItemName() {
        return itemName;
    }

    /**
     * Đặt tên sản phẩm.
     *
     * @param itemName tên sản phẩm mới
     */
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    /**
     * Lấy mô tả sản phẩm.
     *
     * @return mô tả
     */
    public String getDescription() {
        return description;
    }

    /**
     * Đặt mô tả sản phẩm.
     *
     * @param description mô tả mới
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Lấy giá khởi điểm.
     *
     * @return giá khởi điểm
     */
    public double getStartingPrice() {
        return startingPrice;
    }

    /**
     * Đặt giá khởi điểm.
     *
     * @param startingPrice giá khởi điểm mới
     */
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    /**
     * Đặt thời gian bắt đầu đấu giá.
     *
     * @param startTime thời gian bắt đầu
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Lấy thời gian bắt đầu đấu giá.
     *
     * @return thời gian bắt đầu
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Lấy thời gian kết thúc đấu giá.
     *
     * @return thời gian kết thúc
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * Đặt thời gian kết thúc đấu giá.
     *
     * @param endTime thời gian kết thúc
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /**
     * Lấy danh mục của sản phẩm này.
     *
     * @return chuỗi danh mục
     */
    public abstract String getCategory();

    /**
     * In thông tin sản phẩm vào log.
     */
    public void printInfo(){
        logger.info("{}: {} - StartingPrice: {}", itemName, description, startingPrice);
    }

    /**
     * Lấy ID người bán (ID người tạo).
     *
     * @return ID người bán
     */
    public int getSellerId() {
        return this.createdBy;
    }

    /**
     * Lấy đường dẫn hình ảnh cho sản phẩm này.
     *
     * @return đường dẫn hình ảnh
     */
    public String getImagePath() {
        return imagePath;
    }

    /**
     * Đặt đường dẫn hình ảnh cho sản phẩm này.
     *
     * @param imagePath đường dẫn hình ảnh
     */
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
