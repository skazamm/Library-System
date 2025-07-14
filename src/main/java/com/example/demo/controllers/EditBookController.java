package com.example.demo.controllers;

import com.example.demo.controllers.models.Book;
import com.example.demo.controllers.utils.BookDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class EditBookController {

    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField isbnField;
    @FXML private TextField copiesField;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private Button saveButton;
    @FXML private Label messageLabel;

    private Book currentBook;

    @FXML
    public void initialize() {
        statusComboBox.getItems().addAll("Available", "Borrowed", "Reserved", "Lost", "Damaged");
        statusComboBox.setValue("Available");
    }

    public void setBook(Book book) {
        this.currentBook = book;
        if (book != null) {
            titleField.setText(book.getTitle());
            authorField.setText(book.getAuthor());
            isbnField.setText(book.getIsbn());
            statusComboBox.setValue(book.getStatus());
            copiesField.setText(String.valueOf(book.getCopies()));
        }
    }

    @FXML
    private void handleSave() {
        if (currentBook == null) {
            messageLabel.setText("No book selected.");
            return;
        }

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

        // Update the book object
        currentBook.setTitle(title);
        currentBook.setAuthor(author);
        currentBook.setIsbn(isbn);
        currentBook.setStatus(status);
        currentBook.setCopies(copies);

        try {
            boolean success = BookDAO.updateBook(currentBook);
            if (success) {
                showSuccess("Book Updated successfully");
                closeDialog();
            } else {
                messageLabel.setText("Failed to update book. Please check your input or try again.");
            }
        } catch (Exception e) {
            String msg = e.getMessage().toLowerCase();
            if (msg.contains("unique") || msg.contains("duplicate")) {
                messageLabel.setText("This ISBN is already in use.");
            } else if (msg.contains("constraint") || msg.contains("foreign key")) {
                messageLabel.setText("Cannot update: The book is referenced elsewhere.");
            } else {
                messageLabel.setText("Database error: " + e.getMessage());
            }
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
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}
