package org.example.loginregister.server.model.entity.user;

public enum UserStatus {
    ACTIVE, BANNED, DELETED;

    //ACTIVE → có thể đặt bid, tạo phiên, login bình thường
    public boolean isActive() {
        return this == ACTIVE;
    }

}