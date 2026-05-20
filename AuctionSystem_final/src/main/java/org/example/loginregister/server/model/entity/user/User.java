package org.example.loginregister.server.model.entity.user;


import org.example.loginregister.server.common.exception.AuthenticationException;
import org.example.loginregister.server.model.entity.Entity;

public abstract class User extends Entity {
    protected String userName;
    private String email;
    protected String password;
    private String fullName;

    // Khởi tạo mặc định ACTIVE ngay từ đầu
    private UserStatusRecord statusRecord = UserStatusRecord.defaultActive();

    public User( String userName, String password, String email, String fullName){
        super();
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
    }

    public final void updateStatus(UserStatusRecord newRecord) {
        this.statusRecord = newRecord;
        onStatusChanged(newRecord.getStatus());
    }

    //hành vi sau khi thay đổi status
    //để abstract thì admin bắt buộc pk Override mà không có hành vi gì
    public void onStatusChanged(UserStatus newStatus){};



    public void logIn(String name, String password) throws AuthenticationException {

        if (!statusRecord.getStatus().isActive()) {
            throw new AuthenticationException("Account is banned");
        }
        if (!this.userName.equals(name) || !this.password.equals(password)) {
            throw new AuthenticationException("Invalid username or password");
        }
    }



    public UserStatusRecord getStatusRecord() {return statusRecord;}
    public UserStatus getStatus() {return statusRecord.getStatus();}
    public boolean isActive() {return statusRecord.getStatus().isActive();}

    public String getName(){
        return userName;
    }
    public String getEmail(){
        return email;
    }
    public String getPassword(){
        return password;
    }
    public String getFullName(){
        return fullName;
    }
    public void setName(String name){
        this.userName = name;
    }
    public void setEmail(String email){
        this.email = email;
    }
    public void setPassword(String password){
        this.password = password;
    }
    public void setFullName(String fullName){
        this.fullName = fullName;
    }
    public abstract String getRole();
}
