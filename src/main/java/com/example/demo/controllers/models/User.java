package com.example.demo.controllers.models;

import java.sql.Timestamp;
import java.time.LocalDate;

public class User {
    private int userId;
    private String username;
    private String password;
    private String name; // assumed to be full name
    private String role;
    private String email;
    private String contactNumber;
    private LocalDate birthday;
    private Timestamp dateRegistered; // For dashboard "recent users"

    public User(int userId, String username, String password, String name, String role, String email, String contactNumber, LocalDate birthday, Timestamp dateRegistered) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.name = name;
        this.role = role;
        this.email = email;
        this.contactNumber = contactNumber;
        this.birthday = birthday;
        this.dateRegistered = dateRegistered;
    }

    // --- Getters and setters ---

    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNumber() {
        return contactNumber;
    }
    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public LocalDate getBirthday() {
        return birthday;
    }
    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    // For dashboard:
    public Timestamp getDateRegistered() {
        return dateRegistered;
    }
    public void setDateRegistered(Timestamp dateRegistered) {
        this.dateRegistered = dateRegistered;
    }

    // For dashboard compatibility
    public String getFullName() {
        return name; // change if you have separate first/last
    }
}
