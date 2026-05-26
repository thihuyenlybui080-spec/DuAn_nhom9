package vn.edu.vnu.auction.model.entity;

import java.io.Serializable;

public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id = -1; // -1 means not set yet

    protected Entity() {
    }

    // Constructor for loading from database with existing ID
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
