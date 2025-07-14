package com.example.demo.controllers;

import com.example.demo.controllers.utils.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.concurrent.Task;

import java.time.LocalDate;
import java.io.IOException;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField nameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField emailField;
    @FXML private TextField contactNumberField;
    @FXML private ComboBox<String> monthComboBox;
    @FXML private ComboBox<Integer> dateComboBox;
    @FXML private ComboBox<Integer> yearComboBox;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        monthComboBox.getItems().addAll(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        );
        for (int i = 1; i <= 31; i++) dateComboBox.getItems().add(i);
        for (int i = 2025; i >= 1980; i--) yearComboBox.getItems().add(i);
    }

    private void clearFields() {
        usernameField.clear();
        nameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        emailField.clear();
        contactNumberField.clear();
        monthComboBox.getSelectionModel().clearSelection();
        dateComboBox.getSelectionModel().clearSelection();
        yearComboBox.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String name = nameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String email = emailField.getText().trim();
        String contact = contactNumberField.getText().trim();
        String month = monthComboBox.getValue();
        Integer day = dateComboBox.getValue();
        Integer year = yearComboBox.getValue();

        // UI Validations
        if (username.isEmpty() || name.isEmpty() || password.isEmpty() ||
                confirmPassword.isEmpty() || email.isEmpty() || contact.isEmpty() ||
                month == null || day == null || year == null) {
            messageLabel.setText("Please fill in all fields.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        // Run registration in background for UI responsiveness
        messageLabel.setText("Processing registration...");
        Task<Boolean> registerTask = new Task<>() {
            @Override
            protected Boolean call() {
                // 1. Duplicate username check (if UserDAO returns null, username is available)
                if (UserDAO.getUserByUsername(username) != null) {
                    updateMessage("Username already taken. Please choose another.");
                    return false;
                }
                // 2. Compose birthday
                int monthIndex = monthComboBox.getItems().indexOf(month) + 1;
                LocalDate birthday = LocalDate.of(year, monthIndex, day);

                // 3. Register user
                boolean success = UserDAO.register(
                        username, password, name, "student", email, contact, birthday
                );
                if (success) {
                    updateMessage("Registration successful! You can now log in.");
                } else {
                    updateMessage("Registration failed. Please try again.");
                }
                return success;
            }
        };

        // Update messageLabel automatically
        registerTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            messageLabel.setText(newMsg);
        });

        registerTask.setOnSucceeded(e -> {
            if (registerTask.getValue()) {
                clearFields();
            }
        });

        registerTask.setOnFailed(e -> {
            messageLabel.setText("Registration unavailable. Try again later.");
        });

        new Thread(registerTask).start();
    }

    @FXML
    private void handleBack(MouseEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/LoginStudent.fxml"));
        Parent loginPage = loader.load();
        Stage stage = new Stage();
        stage.setScene(new Scene(loginPage));
        stage.show();
        ((Stage) ((Node)event.getSource()).getScene().getWindow()).close();
    }
}
