package vn.edu.vnu.auction.model.entity.user;

import java.io.Serial;
import java.io.Serializable;

public record UserStatusRecord(UserStatus status, Admin changedBy) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static UserStatusRecord defaultActive() {
        return new UserStatusRecord(UserStatus.ACTIVE, null);
    }
}
