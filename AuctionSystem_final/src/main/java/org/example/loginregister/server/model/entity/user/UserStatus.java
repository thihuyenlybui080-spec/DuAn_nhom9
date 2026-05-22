package org.example.loginregister.server.model.entity.user;

public enum UserStatus {
    ACTIVE, BANNED, DELETED;
    public boolean isActive() {
        return this == ACTIVE;
    }

}