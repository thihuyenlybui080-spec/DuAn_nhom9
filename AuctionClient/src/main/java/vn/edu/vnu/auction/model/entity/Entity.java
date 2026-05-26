package vn.edu.vnu.auction.model.entity;

import java.io.Serializable;

public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id = -1;

    protected Entity() {
    }

    // Constructor cho test hoặc load dữ liệu
    protected Entity(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
