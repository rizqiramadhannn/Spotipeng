package com.example.spotipeng.model;

public class LoginPayload {
    private String email;
    private String password;

    public LoginPayload(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}