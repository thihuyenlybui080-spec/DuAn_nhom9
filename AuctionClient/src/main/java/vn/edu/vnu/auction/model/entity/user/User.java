package vn.edu.vnu.auction.model.entity.user;


import vn.edu.vnu.auction.common.exception.AuthenticationException;
import vn.edu.vnu.auction.model.entity.Entity;

/**
 * Lớp cơ sở trừu tượng đại diện cho người dùng trong hệ thống đấu giá.
 * <p>
 * Lớp này cung cấp các thuộc tính và phương thức chung cho tất cả các loại người dùng,
 * bao gồm thông tin đăng nhập, trạng thái tài khoản, và các phương thức xác thực.
 * </p>
 */
public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;
    protected String userName;
    private String email;
    protected String password;
    private String fullName;
    private UserStatusRecord statusRecord = UserStatusRecord.defaultActive();

    /**
     * Khởi tạo một User mới mà không có ID.
     *
     * @param userName tên đăng nhập của người dùng
     * @param password mật khẩu của người dùng
     * @param email địa chỉ email của người dùng
     * @param fullName họ và tên đầy đủ của người dùng
     */
    public User( String userName, String password, String email, String fullName){
        super();
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
    }

    /**
     * Khởi tạo một User mới với ID đã cho.
     *
     * @param id ID của người dùng
     * @param userName tên đăng nhập của người dùng
     * @param password mật khẩu của người dùng
     * @param email địa chỉ email của người dùng
     * @param fullName họ và tên đầy đủ của người dùng
     */
    public User(int id, String userName, String password, String email, String fullName){
        super(id);
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
    }

    /**
     * Cập nhật trạng thái của người dùng.
     *
     * @param newRecord bản ghi trạng thái mới
     */
    public final void updateStatus(UserStatusRecord newRecord) {
        this.statusRecord = newRecord;
        onStatusChanged(newRecord.getStatus());
    }

    /**
     * Phương thức callback được gọi khi trạng thái người dùng thay đổi.
     * Có thể được ghi đè bởi các lớp con để xử lý logic cụ thể.
     *
     * @param newStatus trạng thái mới của người dùng
     */
    public void onStatusChanged(UserStatus newStatus){};

    /**
     * Xác thực đăng nhập của người dùng.
     *
     * @param name tên đăng nhập để xác thực
     * @param password mật khẩu để xác thực
     * @throws AuthenticationException nếu tên đăng nhập hoặc mật khẩu không đúng,
     *                                  hoặc nếu tài khoản bị khóa
     */
    public void logIn(String name, String password) throws AuthenticationException {

        if (!statusRecord.getStatus().isActive()) {
            throw new AuthenticationException("Account is banned");
        }
        if (!this.userName.equals(name) || !this.password.equals(password)) {
            throw new AuthenticationException("Invalid username or password");
        }
    }

    /**
     * Lấy bản ghi trạng thái của người dùng.
     *
     * @return bản ghi trạng thái hiện tại
     */
    public UserStatusRecord getStatusRecord() {return statusRecord;}

    /**
     * Lấy trạng thái của người dùng.
     *
     * @return trạng thái hiện tại của người dùng
     */
    public UserStatus getStatus() {return statusRecord.getStatus();}

    /**
     * Kiểm tra xem tài khoản người dùng có đang hoạt động không.
     *
     * @return true nếu tài khoản đang hoạt động, false nếu bị khóa
     */
    public boolean isActive() {return statusRecord.getStatus().isActive();}

    /**
     * Đặt trạng thái hoạt động của người dùng.
     *
     * @param active true để kích hoạt tài khoản, false để khóa tài khoản
     */
    public void setActive(boolean active) {
        if (active) {
            this.statusRecord = UserStatusRecord.defaultActive();
        } else {
            this.statusRecord = new UserStatusRecord(UserStatus.BANNED, null);
        }
        onStatusChanged(statusRecord.getStatus());
    }

    /**
     * Lấy tên đăng nhập của người dùng.
     *
     * @return tên đăng nhập
     */
    public String getName(){
        return userName;
    }

    /**
     * Lấy địa chỉ email của người dùng.
     *
     * @return địa chỉ email
     */
    public String getEmail(){
        return email;
    }

    /**
     * Lấy mật khẩu của người dùng.
     *
     * @return mật khẩu
     */
    public String getPassword(){
        return password;
    }

    /**
     * Lấy họ và tên đầy đủ của người dùng.
     *
     * @return họ và tên đầy đủ
     */
    public String getFullName(){
        return fullName;
    }

    /**
     * Đặt tên đăng nhập mới cho người dùng.
     *
     * @param name tên đăng nhập mới
     */
    public void setName(String name){
        this.userName = name;
    }

    /**
     * Đặt địa chỉ email mới cho người dùng.
     *
     * @param email địa chỉ email mới
     */
    public void setEmail(String email){
        this.email = email;
    }

    /**
     * Đặt mật khẩu mới cho người dùng.
     *
     * @param password mật khẩu mới
     */
    public void setPassword(String password){
        this.password = password;
    }

    /**
     * Đặt họ và tên đầy đủ mới cho người dùng.
     *
     * @param fullName họ và tên đầy đủ mới
     */
    public void setFullName(String fullName){
        this.fullName = fullName;
    }

    /**
     * Lấy vai trò của người dùng.
     * Phương thức trừu tượng phải được triển khai bởi các lớp con.
     *
     * @return chuỗi đại diện cho vai trò của người dùng
     */
    public abstract String getRole();
}
