package com.example.TERA;

public class Admin {
    public String username;
    public String email;
    public String pass;
    public String avatar;
    public String phoneNumber;

    public Admin() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Admin(String username,String email, String pass, String avatar, String phoneNumber) {
        this.username = username;
        this.email = email;
        this.pass = pass;
        this.avatar = avatar;
        this.phoneNumber = phoneNumber;
    }
}
