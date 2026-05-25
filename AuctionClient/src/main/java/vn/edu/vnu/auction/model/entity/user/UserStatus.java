package vn.edu.vnu.auction.model.entity.user;

public enum UserStatus {
    ACTIVE, BANNED, DELETED;
    public boolean isActive() {
        return this == ACTIVE;
    }

}