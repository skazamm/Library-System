package com.example.demo.controllers.utils;

import com.example.demo.controllers.models.User;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // Login: returns User if found, else null
    public static User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                java.sql.Date sqlDate = rs.getDate("birthday");
                LocalDate birthday = (sqlDate != null) ? sqlDate.toLocalDate() : null;
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("name"),
                        rs.getString("role"),
                        rs.getString("email"),
                        rs.getString("contact_number"),
                        birthday,
                        rs.getTimestamp("date_registered") // New
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Register a new user: returns true if success, false on error (like duplicate username/email)
    public static boolean register(String username, String password, String name, String role, String email, String contact, LocalDate birthday) {
        String sql = "INSERT INTO users (username, password, name, role, email, contact_number, birthday, date_registered) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, name);
            stmt.setString(4, role);
            stmt.setString(5, email);
            stmt.setString(6, contact);
            stmt.setDate(7, java.sql.Date.valueOf(birthday));
            int rowsInserted = stmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Get user by username (useful for checking duplicates, etc.)
    public static User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                java.sql.Date sqlDate = rs.getDate("birthday");
                LocalDate birthday = (sqlDate != null) ? sqlDate.toLocalDate() : null;
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("name"),
                        rs.getString("role"),
                        rs.getString("email"),
                        rs.getString("contact_number"),
                        birthday,
                        rs.getTimestamp("date_registered") // New
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- NEW: For Dashboard - Get the most recently registered users ---
    public static List<User> getRecentUsers(int limit) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY date_registered DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                java.sql.Date sqlDate = rs.getDate("birthday");
                LocalDate birthday = (sqlDate != null) ? sqlDate.toLocalDate() : null;
                users.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("name"),
                        rs.getString("role"),
                        rs.getString("email"),
                        rs.getString("contact_number"),
                        birthday,
                        rs.getTimestamp("date_registered") // New
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
    // ----------------------------------------------------------

    // Returns true if username and password are found in the database
    public static boolean validateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // returns true if a record exists
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Student Login (role = student)
    public static User loginStudent(String username, String password) {
        return loginByRole(username, password, "student");
    }

    // Librarian/ Faculty login role
    public static User loginLibrarian(String username, String password) {
        return loginByRole(username, password, "librarian");
    }

    // Generic role-based login
    private static User loginByRole(String username, String password, String role) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND role = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                java.sql.Date sqlDate = rs.getDate("birthday");
                LocalDate birthday = (sqlDate != null) ? sqlDate.toLocalDate() : null;
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("name"),
                        rs.getString("role"),
                        rs.getString("email"),
                        rs.getString("contact_number"),
                        birthday,
                        rs.getTimestamp("date_registered") // New
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
