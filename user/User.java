package user;
public abstract class User {
    private String userName;
    private int id;
    private String email;
    private String password;
    private String fullName;
    public User(){}
    public User(String userName, int id, String password, String email, String fullName){
        this.userName = userName;
        this.id = id;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
    }
    public String getName(){
        return userName;
    }
    public int getId(){
        return id;
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
        System.out.println(name);
    }
    public void setId(int id){
        this.id = id;
        System.out.println(id);
    }
    public void setEmail(String email){
        this.email = email;
        System.out.println(email);
    }
    public void setPassword(String password){
        this.password = password;
        System.out.println(password);
    }
    public void setFullname(String fullName){
        this.fullName = fullName;
        System.out.println(fullName);
    }
    public abstract void logIn(String name, String password);
}