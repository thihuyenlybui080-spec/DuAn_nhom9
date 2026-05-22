package org.example.loginregister.server.model.entity.user;

import java.io.Serializable;

public class UserStatusRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    private final UserStatus status;
    private final Admin changedBy;

    public UserStatusRecord(UserStatus status, Admin changedBy) {
        this.status= status;
        this.changedBy = changedBy;
    }

    //Record khởi tạo mặc định khi user mới đăng ký
    public static UserStatusRecord defaultActive() {
        return new UserStatusRecord(UserStatus.ACTIVE,null);
    }

    public UserStatus getStatus(){ return status; }
    public Admin getChangedBy() { return changedBy; }
}
