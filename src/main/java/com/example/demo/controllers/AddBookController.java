package com.example.demo.controllers;

import com.example.demo.controllers.models.Book;
import com.example.demo.controllers.utils.BookDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Timestamp;

public class AddBookController {

    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField isbnField;
    @FXML private TextField copiesField;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private Button addButton;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        statusComboBox.getItems().addAll("Available", "Borrowed", "Reserved", "Lost", "Damaged");
        statusComboBox.setValue("Available");
    }

    @FXML
    private void handleAddBook() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String isbn = isbnField.getText().trim();
        String status = statusComboBox.getValue();
        String copiesStr = copiesField.getText().trim();

        if (title.isEmpty() || author.isEmpty() || isbn.isEmpty() || status == null || status.isEmpty() || copiesStr.isEmpty()) {
            messageLabel.setText("All fields are required.");
            return;
        }

        int copies;
        try {
            copies = Integer.parseInt(copiesStr);
        } catch (NumberFormatException e) {
            messageLabel.setText("Copies must be a number.");
            return;
        }

        // Correct: Use LocalDateTime for Timestamp
        Timestamp dateAdded = Timestamp.valueOf(java.time.LocalDateTime.now());

        Book newBook = new Book(0, title, author, isbn, status, copies, dateAdded);

        boolean success = BookDAO.addBook(newBook);
        if (success) {
            showSuccess("Book added successfully.");
            closeDialog();
        } else {
            messageLabel.setText("Failed to add book. Please check your input or try again.");
        }
    }

    // Add this helper for pop-up alerts!
    private void showSuccess(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void closeDialog() {
        Stage stage = (Stage) addButton.getScene().getWindow();
        stage.close();
    }

}
