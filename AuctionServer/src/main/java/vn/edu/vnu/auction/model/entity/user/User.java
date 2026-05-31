package vn.edu.vnu.auction.model.entity.user;


import vn.edu.vnu.auction.common.exception.AuthenticationException;
import vn.edu.vnu.auction.model.entity.Entity;

import java.io.Serial;

/**
 * Lớp cơ sở trừu tượng cho tất cả các loại người dùng trong hệ thống.
 * <p>
 * Đại diện cho người dùng với thông tin đăng nhập, trạng thái, và vai trò.
 * Các lớp con định nghĩa các vai trò người dùng cụ thể như Admin, Bidder, và Seller.
 * </p>
 */
public abstract class User extends Entity {
    @Serial
    private static final long serialVersionUID = 1L;
    protected String userName;
    private String email;
    protected String password;
    private final String fullName;
    private UserStatusRecord statusRecord = UserStatusRecord.defaultActive();

    /**
     * Tạo một User mới không có ID.
     *
     * @param userName tên người dùng
     * @param password mật khẩu
     * @param email địa chỉ email
     * @param fullName họ tên đầy đủ
     */
    public User( String userName, String password, String email, String fullName){
        super();
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
    }

    /**
     * Tạo một User mới với ID cụ thể.
     *
     * @param id ID người dùng
     * @param userName tên người dùng
     * @param password mật khẩu
     * @param email địa chỉ email
     * @param fullName họ tên đầy đủ
     */
    public User(int id, String userName, String password, String email, String fullName){
        super(id);
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
    }

    /**
     * Cập nhật bản ghi trạng thái người dùng.
     *
     * @param newRecord bản ghi trạng thái mới
     */
    public final void updateStatus(UserStatusRecord newRecord) {
        this.statusRecord = newRecord;
    }

    /**
     * Lấy trạng thái người dùng.
     *
     * @return trạng thái
     */
    public UserStatus getStatus() {return statusRecord.status();}

    /**
     * Kiểm tra xem người dùng có hoạt động không.
     *
     * @return true nếu hoạt động, false nếu không
     */
    public boolean isActive() {return statusRecord.status().isActive();}

    /**
     * Lấy tên người dùng.
     *
     * @return tên người dùng
     */
    public String getName(){
        return userName;
    }


    /**
     * Lấy họ tên đầy đủ.
     *
     * @return họ tên đầy đủ
     */
    public String getFullName(){
        return fullName;
    }

    /**
     * Đặt tên người dùng.
     *
     * @param name tên người dùng mới
     */
    public void setName(String name){
        this.userName = name;
    }

    /**
     * Lấy vai trò người dùng.
     *
     * @return chuỗi vai trò
     */
    public abstract String getRole();
}
