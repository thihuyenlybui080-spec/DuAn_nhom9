package org.example.loginregister.server.model.entity;

import java.io.Serializable;
import java.util.UUID;

public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    protected Entity() {
        this.id = generateId();
    }

    // Constructor cho test hoặc load dữ liệu
    protected Entity(String customId) {
        this.id = (customId == null || customId.trim().isEmpty())
                ? generateId() : customId;
    }

    //sinh id ngẫu nhiên
    private String generateId() {
        String prefix = getIdPrefix();
        String randomPart = UUID.randomUUID().toString().substring(0, 8); // 8 ký tự
        return prefix + "-" + randomPart;
    }
    // Mỗi subclass override phương thức này để định nghĩa prefix
    protected abstract String getIdPrefix();


    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
}
