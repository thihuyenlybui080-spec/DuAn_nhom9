package vn.edu.vnu.auction.model.entity;

import java.io.Serializable;

/**
 * Lớp cơ sở cho tất cả các lớp entity trong hệ thống đấu giá.
 * <p>
 * Cung cấp chức năng chung cho các entity bao gồm định danh duy nhất.
 * Tất cả entity đều có thể serialize để hỗ trợ truyền qua mạng.
 * </p>
 */
public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id = -1;

    /**
     * Constructor mặc định để tạo entity mới không có ID.
     */
    protected Entity() {
    }

    /**
     * Constructor để tạo entity với ID cụ thể.
     *
     * @param id định danh duy nhất cho entity này
     */
    protected Entity(int id) {
        this.id = id;
    }

    /**
     * Lấy định danh duy nhất của entity này.
     *
     * @return ID entity
     */
    public int getId() {
        return id;
    }

    /**
     * Đặt định danh duy nhất cho entity này.
     *
     * @param id ID entity mới
     */
    public void setId(int id) {
        this.id = id;
    }
}
