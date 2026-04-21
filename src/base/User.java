package base;

import exceptions.AuthenticationException;

public abstract class User extends Entity {
    protected String userName;
    private String email;
    protected String password;
    private String fullName;
    public User(String id, String userName, String password, String email, String fullName){
        super(id);
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
    }
    public String getName(){
        return userName;
    }
    public String getEmail(){
        return email;
    }public String getPassword(){
        return password;
    }
    public String getFullname(){
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
    public void setFullname(String fullName){
        this.fullName = fullName;
    }
    public abstract void logIn(String name, String password) throws AuthenticationException;
}