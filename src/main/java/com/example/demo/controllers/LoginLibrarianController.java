package com.example.demo.controllers;

import com.example.demo.controllers.models.User;
import com.example.demo.controllers.utils.UserDAO;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.prefs.Preferences;

public class LoginLibrarianController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button loginButton;
    @FXML private CheckBox rememberMeCheckBox;

    // Java Preferences for "Remember Me"
    private static final Preferences PREFS = Preferences.userNodeForPackage(LoginLibrarianController.class);

    @FXML
    public void initialize() {
        // Load saved preferences (username and password)
        String savedUsername = PREFS.get("librarian_username", "");
        String savedPassword = PREFS.get("librarian_password", "");
        boolean rememberMe = PREFS.getBoolean("librarian_rememberMe", false);

        if (usernameField != null) usernameField.setText(savedUsername);
        if (passwordField != null) passwordField.setText(savedPassword);
        if (rememberMeCheckBox != null) rememberMeCheckBox.setSelected(rememberMe);

        if (progressIndicator != null) progressIndicator.setVisible(false);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Username and password are required!");
            return;
        }

        messageLabel.setText("Logging in, please wait...");
        setSpinner(true);
        setLoginDisabled(true);

        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() {
                try {
                    return UserDAO.loginLibrarian(username, password);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };

        loginTask.setOnSucceeded(e -> {
            setSpinner(false);
            setLoginDisabled(false);
            User librarian = loginTask.getValue();
            if (librarian != null) {
                // Save or clear preferences (both username and password)
                if (rememberMeCheckBox != null && rememberMeCheckBox.isSelected()) {
                    PREFS.put("librarian_username", username);
                    PREFS.put("librarian_password", password); // Only for demo
                    PREFS.putBoolean("librarian_rememberMe", true);
                } else {
                    PREFS.remove("librarian_username");
                    PREFS.remove("librarian_password");
                    PREFS.putBoolean("librarian_rememberMe", false);
                }

                // Login success: open dashboard
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/LibraryManagement.fxml"));
                    Parent page = loader.load();
                    Stage stage = new Stage();
                    stage.setScene(new Scene(page));
                    stage.setTitle("Librarian Management");
                    stage.show();
                    ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    messageLabel.setText("Error loading dashboard.");
                }
            } else {
                messageLabel.setText("Invalid username or password. Please try again.");
            }
        });

        loginTask.setOnFailed(e -> {
            setSpinner(false);
            setLoginDisabled(false);
            messageLabel.setText("Login unavailable. Please check your connection and try again.");
        });

        new Thread(loginTask).start();
    }

    private void setSpinner(boolean visible) {
        if (progressIndicator != null) {
            Platform.runLater(() -> progressIndicator.setVisible(visible));
        }
    }

    private void setLoginDisabled(boolean disabled) {
        if (loginButton != null) {
            Platform.runLater(() -> loginButton.setDisable(disabled));
        }
        if (usernameField != null) {
            Platform.runLater(() -> usernameField.setDisable(disabled));
        }
        if (passwordField != null) {
            Platform.runLater(() -> passwordField.setDisable(disabled));
        }
        if (rememberMeCheckBox != null) {
            Platform.runLater(() -> rememberMeCheckBox.setDisable(disabled));
        }
    }

    @FXML
    private void handleBack(MouseEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/FrontPage.fxml"));
        Parent page = loader.load();
        Stage stage = new Stage();
        stage.setScene(new Scene(page));
        stage.setTitle("Welcome");
        stage.show();

        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
    }
}
