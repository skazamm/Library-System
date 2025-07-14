package com.example.demo.controllers;

import com.example.demo.controllers.models.User;
import com.example.demo.controllers.utils.UserDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.util.prefs.Preferences;

public class LoginStudentController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private ImageView backImage;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private CheckBox rememberMeCheckBox;

    // Java Preferences for "Remember Me"
    private static final Preferences PREFS = Preferences.userNodeForPackage(LoginStudentController.class);

    @FXML
    public void initialize() {
        // Load preferences for username and password (for demo; not secure for production!)
        String savedUsername = PREFS.get("student_username", "");
        String savedPassword = PREFS.get("student_password", "");
        boolean rememberMe = PREFS.getBoolean("student_rememberMe", false);

        if (usernameField != null) usernameField.setText(savedUsername);
        if (passwordField != null) passwordField.setText(savedPassword);
        if (rememberMeCheckBox != null) rememberMeCheckBox.setSelected(rememberMe);

        if (backImage != null) {
            backImage.setCursor(javafx.scene.Cursor.HAND);
            backImage.setOnMouseClicked(event -> {
                try {
                    handleBack(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
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

        Platform.runLater(() -> messageLabel.setText("Logging in, please wait..."));
        setSpinner(true);
        setInputDisabled(true);

        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() {
                try {
                    return UserDAO.loginStudent(username, password); // Get User, not just a boolean!
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };

        loginTask.setOnSucceeded(e -> {
            User user = loginTask.getValue();
            setSpinner(false);
            setInputDisabled(false);

            if (user != null) {
                // Save credentials if "Remember Me" is checked
                if (rememberMeCheckBox.isSelected()) {
                    PREFS.put("student_username", username);
                    PREFS.put("student_password", password);  // Warning: plain text, for demo/testing only!
                    PREFS.putBoolean("student_rememberMe", true);
                } else {
                    PREFS.remove("student_username");
                    PREFS.remove("student_password");
                    PREFS.putBoolean("student_rememberMe", false);
                }

                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/StudentManagement.fxml"));
                    Parent page = loader.load();

                    // Pass the User to the controller
                    StudentManagementController controller = loader.getController();
                    controller.setCurrentUser(user);

                    Stage stage = new Stage();
                    stage.setScene(new Scene(page));
                    stage.setTitle("Student Dashboard");
                    stage.show();
                    ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    messageLabel.setText("Error loading dashboard.");
                }
            } else {
                messageLabel.setText("Invalid username or password.");
            }
        });

        loginTask.setOnFailed(e -> {
            setSpinner(false);
            setInputDisabled(false);
            messageLabel.setText("Login unavailable. Please check your connection and try again.");
        });

        new Thread(loginTask).start();
    }

    private void setSpinner(boolean visible) {
        if (progressIndicator != null) {
            Platform.runLater(() -> progressIndicator.setVisible(visible));
        }
    }
    private void setInputDisabled(boolean disabled) {
        Platform.runLater(() -> {
            if (usernameField != null) usernameField.setDisable(disabled);
            if (passwordField != null) passwordField.setDisable(disabled);
            if (rememberMeCheckBox != null) rememberMeCheckBox.setDisable(disabled);
        });
    }

    @FXML
    private void handleRegister(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/Register.fxml"));
        Parent registerPage = loader.load();
        Stage registerStage = new Stage();
        registerStage.setScene(new Scene(registerPage));
        registerStage.setTitle("Register");
        registerStage.show();
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
    }

    @FXML
    private void handleBack(MouseEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/FrontPage.fxml"));
        Parent mainPage = loader.load();
        Stage stage = new Stage();
        stage.setScene(new Scene(mainPage));
        stage.setTitle("Welcome");
        stage.show();
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
    }
}
