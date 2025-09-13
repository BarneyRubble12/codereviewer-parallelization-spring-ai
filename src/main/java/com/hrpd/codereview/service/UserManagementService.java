package com.hrpd.codereview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing users - contains various security and clean code issues for testing
 */
@Slf4j
@Service
public class UserManagementService {

    // SECURITY ISSUE: Hardcoded credentials
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/mydb";
    private static final String DB_USER = "admin";
    private static final String DB_PASSWORD = "super_secret_password_123";
    private static final String API_KEY = "sk-1234567890abcdef1234567890abcdef";
    
    // CLEAN CODE ISSUE: Poor naming conventions
    private String x = "";
    private int cnt = 0;
    private List<String> lst = new ArrayList<>();

    /**
     * Creates a new user with the provided information
     * @param username the username
     * @param password the password
     * @param email the email address
     * @param phone the phone number
     * @param ssn the social security number
     * @return true if successful
     */
    public boolean createUser(String username, String password, String email, String phone, String ssn) {
        try {
            // SECURITY ISSUE: Insecure logging of sensitive data
            log.info("Creating user with username: {}, password: {}, email: {}, phone: {}, ssn: {}", 
                    username, password, email, phone, ssn);
            
            // CLEAN CODE ISSUE: Missing input validation
            if (username == null) {
                return false;
            }
            
            // SECURITY ISSUE: SQL injection vulnerability
            String query = "INSERT INTO users (username, password, email, phone, ssn) VALUES ('" + 
                          username + "', '" + password + "', '" + email + "', '" + phone + "', '" + ssn + "')";
            
            // CLEAN CODE ISSUE: Deep nesting and long method
            if (username.length() > 0) {
                if (password != null && password.length() > 0) {
                    if (email != null && email.contains("@")) {
                        if (phone != null && phone.length() > 0) {
                            if (ssn != null && ssn.length() > 0) {
                                // SECURITY ISSUE: Insecure HTTP communication
                                String apiUrl = "http://external-api.com/validate-user";
                                String response = callExternalApi(apiUrl, username);
                                
                                if (response != null) {
                                    if (response.contains("valid")) {
                                        if (executeQuery(query)) {
                                            // CLEAN CODE ISSUE: Code duplication
                                            log.info("User created successfully");
                                            sendNotificationEmail(email, "Welcome!");
                                            sendNotificationSms(phone, "Welcome!");
                                            return true;
                                        } else {
                                            log.error("Failed to create user in database");
                                            return false;
                                        }
                                    } else {
                                        log.error("External API validation failed");
                                        return false;
                                    }
                                } else {
                                    log.error("External API call failed");
                                    return false;
                                }
                            } else {
                                log.error("SSN is required");
                                return false;
                            }
                        } else {
                            log.error("Phone is required");
                            return false;
                        }
                    } else {
                        log.error("Valid email is required");
                        return false;
                    }
                } else {
                    log.error("Password is required");
                    return false;
                }
            } else {
                log.error("Username is required");
                return false;
            }
        } catch (Exception e) {
            // CLEAN CODE ISSUE: Generic exception handling
            log.error("Error occurred");
            return false;
        }
    }

    /**
     * Retrieves user information by ID
     */
    public String getUserById(String userId) {
        try {
            // SECURITY ISSUE: SQL injection vulnerability
            String query = "SELECT * FROM users WHERE id = " + userId;
            
            // CLEAN CODE ISSUE: Poor variable naming
            String x = executeQueryAndReturn(query);
            
            // SECURITY ISSUE: Logging sensitive information
            log.info("Retrieved user data: {}", x);
            
            return x;
        } catch (Exception e) {
            // CLEAN CODE ISSUE: Swallowing exceptions
            return null;
        }
    }

    /**
     * Updates user password
     */
    public boolean updatePassword(String userId, String newPassword) {
        // SECURITY ISSUE: No password validation
        // SECURITY ISSUE: SQL injection
        String query = "UPDATE users SET password = '" + newPassword + "' WHERE id = " + userId;
        
        // CLEAN CODE ISSUE: Code duplication (same logging pattern as createUser)
        log.info("Updating password for user: {}", userId);
        log.info("New password: {}", newPassword);
        
        try {
            return executeQuery(query);
        } catch (Exception e) {
            // CLEAN CODE ISSUE: Generic exception handling
            return false;
        }
    }

    /**
     * Deletes a user
     */
    public boolean deleteUser(String userId) {
        // SECURITY ISSUE: SQL injection
        String query = "DELETE FROM users WHERE id = " + userId;
        
        // CLEAN CODE ISSUE: Code duplication
        log.info("Deleting user: {}", userId);
        
        try {
            return executeQuery(query);
        } catch (Exception e) {
            // CLEAN CODE ISSUE: Generic exception handling
            return false;
        }
    }

    /**
     * Validates user credentials
     */
    public boolean validateUser(String username, String password) {
        // SECURITY ISSUE: SQL injection
        String query = "SELECT password FROM users WHERE username = '" + username + "'";
        
        try {
            String storedPassword = executeQueryAndReturn(query);
            
            // SECURITY ISSUE: Weak password comparison (timing attack)
            if (storedPassword != null && storedPassword.equals(password)) {
                // SECURITY ISSUE: Logging sensitive information
                log.info("User {} authenticated successfully with password: {}", username, password);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            // CLEAN CODE ISSUE: Generic exception handling
            return false;
        }
    }

    /**
     * Gets all users - contains performance issues
     */
    public List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        
        try {
            // SECURITY ISSUE: SQL injection
            String query = "SELECT * FROM users";
            
            // CLEAN CODE ISSUE: Poor variable naming and inefficient code
            String x = executeQueryAndReturn(query);
            if (x != null) {
                String[] parts = x.split("\n");
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];
                    if (part != null && part.length() > 0) {
                        users.add(part);
                    }
                }
            }
            
            // SECURITY ISSUE: Logging all user data
            log.info("Retrieved all users: {}", users);
            
        } catch (Exception e) {
            // CLEAN CODE ISSUE: Generic exception handling
        }
        
        return users;
    }

    // Helper methods with various issues

    private boolean executeQuery(String query) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();
            stmt.execute(query);
            stmt.close();
            conn.close();
            return true;
        } catch (Exception e) {
            // CLEAN CODE ISSUE: Generic exception handling
            return false;
        }
    }

    private String executeQueryAndReturn(String query) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            StringBuilder result = new StringBuilder();
            while (rs.next()) {
                result.append(rs.getString(1)).append("\n");
            }
            
            rs.close();
            stmt.close();
            conn.close();
            
            return result.toString();
        } catch (Exception e) {
            // CLEAN CODE ISSUE: Generic exception handling
            return null;
        }
    }

    private String callExternalApi(String url, String data) {
        try {
            // SECURITY ISSUE: Insecure HTTP communication
            // CLEAN CODE ISSUE: Poor variable naming
            String x = "http://" + url.replace("https://", "");
            
            // Simulate API call
            Thread.sleep(1000);
            return "valid";
        } catch (Exception e) {
            // CLEAN CODE ISSUE: Generic exception handling
            return null;
        }
    }

    // CLEAN CODE ISSUE: Code duplication
    private void sendNotificationEmail(String email, String message) {
        log.info("Sending email to {}: {}", email, message);
        // Simulate email sending
    }

    private void sendNotificationSms(String phone, String message) {
        log.info("Sending SMS to {}: {}", phone, message);
        // Simulate SMS sending
    }

    // CLEAN CODE ISSUE: Unused method
    private void unusedMethod() {
        int x = 0;
        int y = 0;
        int z = x + y;
        log.info("Unused method result: {}", z);
    }
}
