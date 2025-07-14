package com.example.demo.controllers;

import com.example.demo.controllers.models.Book;
import com.example.demo.controllers.models.BorrowedBook;
import com.example.demo.controllers.utils.BookDAO;
import com.example.demo.controllers.utils.BorrowedBookDAO;
import com.example.demo.controllers.utils.TransactionDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LibrarianManagementController {

    @FXML private TabPane tabPane;
    @FXML private Tab bookDetailsTab;
    @FXML private Tab borrowedBooksTab;

    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, String> isbnColumn;
    @FXML private TableColumn<Book, String> statusColumn;
    @FXML private TableColumn<Book, Integer> copiesColumn;

    @FXML private TableView<BorrowedBook> borrowedBooksTable;
    @FXML private TableColumn<BorrowedBook, String> borrowedBookTitleColumn;
    @FXML private TableColumn<BorrowedBook, String> borrowerNameColumn;
    @FXML private TableColumn<BorrowedBook, String> borrowDateColumn;
    @FXML private TableColumn<BorrowedBook, String> dueDateColumn;
    @FXML private TableColumn<BorrowedBook, String> borrowedStatusColumn;

    @FXML private Button addBookButton, editBookButton, deleteBookButton, returnBookButton;
    @FXML private TextField searchField;
    @FXML private ProgressIndicator progressIndicator;

    private final ObservableList<Book> bookList = FXCollections.observableArrayList();
    private final ObservableList<BorrowedBook> borrowedBooksList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Book Table columns
        titleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
        authorColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAuthor()));
        isbnColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getIsbn()));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));
        copiesColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getCopies()).asObject());
        bookTable.setItems(bookList);

        // Borrowed Books Table columns
        borrowedBookTitleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBookTitle()));
        borrowerNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBorrowerName()));
        borrowDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBorrowDate()));
        dueDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDueDate()));
        borrowedStatusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));
        borrowedBooksTable.setItems(borrowedBooksList);

        // Initial loading
        loadBooksInBackground();
        loadBorrowedBooksInBackground();

        // Live search for BOTH tabs (context-aware)
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> {
                if (tabPane.getSelectionModel().getSelectedItem() == bookDetailsTab) {
                    searchBooksInBackground(newV);
                } else if (tabPane.getSelectionModel().getSelectedItem() == borrowedBooksTab) {
                    searchBorrowedBooksInBackground(newV);
                }
            });
        }

        // Optional: clear search field when switching tabs
        if (tabPane != null) {
            tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (searchField != null) searchField.clear();
                updateActionButtonsState();
            });
        }

        if (progressIndicator != null) progressIndicator.setVisible(false);

        // UI logic: Return Book only enabled on correct tab and row selection
        if (tabPane != null && borrowedBooksTable != null) {
            tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> updateActionButtonsState());
            borrowedBooksTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> updateActionButtonsState());
        }
        updateActionButtonsState();
    }

    // Button enable/disable logic based on selected tab
    private void updateActionButtonsState() {
        boolean onBookDetails = tabPane.getSelectionModel().getSelectedItem() == bookDetailsTab;
        boolean onBorrowedBooks = tabPane.getSelectionModel().getSelectedItem() == borrowedBooksTab;

        if (addBookButton != null) addBookButton.setDisable(!onBookDetails);
        if (editBookButton != null) editBookButton.setDisable(!onBookDetails);
        if (deleteBookButton != null) deleteBookButton.setDisable(!onBookDetails);

        // Return Book only if a row is selected AND on Borrowed Books tab
        if (returnBookButton != null) {
            boolean rowSelected = borrowedBooksTable.getSelectionModel().getSelectedItem() != null;
            returnBookButton.setDisable(!(onBorrowedBooks && rowSelected));
        }
    }

    private void updateReturnBookButtonState() {
        boolean borrowedTabActive = tabPane.getSelectionModel().getSelectedItem() == borrowedBooksTab;
        boolean rowSelected = borrowedBooksTable.getSelectionModel().getSelectedItem() != null;
        if (returnBookButton != null) {
            returnBookButton.setDisable(!(borrowedTabActive && rowSelected));
        }
    }

    // Add this method in your controller:
    private void handleSearch(String keyword) {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == bookDetailsTab) {
            searchBooksInBackground(keyword);
        } else if (selectedTab == borrowedBooksTab) {
            searchBorrowedBooksInBackground(keyword);
        }
    }

    private void searchBorrowedBooksInBackground(String keyword) {
        setSpinner(true);
        Task<List<BorrowedBook>> searchTask = new Task<>() {
            @Override
            protected List<BorrowedBook> call() {
                if (keyword == null || keyword.isBlank())
                    return BorrowedBookDAO.getAllBorrowedBooks();
                else
                    return BorrowedBookDAO.searchBorrowedBooks(keyword);
            }
        };
        searchTask.setOnSucceeded(e -> {
            List<BorrowedBook> result = searchTask.getValue();
            borrowedBooksList.setAll(result != null ? result : new ArrayList<>());
            setSpinner(false);
        });
        searchTask.setOnFailed(e -> {
            borrowedBooksList.clear();
            showAlert("Error", "Failed to search borrowed books.");
            setSpinner(false);
        });
        new Thread(searchTask).start();
    }

    private void loadBooksInBackground() {
        setSpinner(true);
        Task<List<Book>> task = new Task<>() {
            @Override
            protected List<Book> call() {
                return BookDAO.getAllBooks();
            }
        };
        task.setOnSucceeded(event -> {
            // Defensive null handling
            List<Book> result = task.getValue();
            bookList.setAll(result != null ? result : new ArrayList<>());
            setSpinner(false);
        });
        task.setOnFailed(event -> {
            bookList.clear();
            showAlert("Error", "Failed to load books.");
            setSpinner(false);
        });
        new Thread(task).start();
    }

    private void searchBooksInBackground(String keyword) {
        setSpinner(true);
        Task<List<Book>> searchTask = new Task<>() {
            @Override
            protected List<Book> call() {
                if (keyword == null || keyword.isBlank()) return BookDAO.getAllBooks();
                return BookDAO.searchBooks(keyword);
            }
        };
        searchTask.setOnSucceeded(e -> {
            List<Book> result = searchTask.getValue();
            bookList.setAll(result != null ? result : new ArrayList<>());
            setSpinner(false);
        });
        searchTask.setOnFailed(e -> {
            bookList.clear();
            showAlert("Error", "Failed to search books.");
            setSpinner(false);
        });
        new Thread(searchTask).start();
    }

    private void loadBorrowedBooksInBackground() {
        setSpinner(true);
        Task<List<BorrowedBook>> task = new Task<>() {
            @Override
            protected List<BorrowedBook> call() {
                return BorrowedBookDAO.getAllBorrowedBooks();
            }
        };
        task.setOnSucceeded(event -> {
            List<BorrowedBook> result = task.getValue();
            borrowedBooksList.setAll(result != null ? result : new ArrayList<>());
            setSpinner(false);
        });
        task.setOnFailed(event -> {
            borrowedBooksList.clear();
            showAlert("Error", "Failed to load borrowed books.");
            setSpinner(false);
        });
        new Thread(task).start();
    }


    @FXML
    private void handleAddBook() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/AddBook.fxml"));
            Parent root = loader.load();

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setTitle("Add New Book");
            dialog.showAndWait();

            loadBooksInBackground();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Unable to open Add Book dialog.");
        }
    }

    @FXML
    private void handleEditBook() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert("No Selection", "Please select a book to edit.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/EditBook.fxml"));
            Parent root = loader.load();

            EditBookController controller = loader.getController();
            controller.setBook(selectedBook);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setTitle("Edit Book");
            dialog.showAndWait();

            loadBooksInBackground();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Unable to open Edit Book dialog.");
        }
    }

    @FXML
    private void handleDeleteBook() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert("No Selection", "Please select a book to delete.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Book");
        alert.setHeaderText("Are you sure you want to delete this book?");
        alert.setContentText(selectedBook.getTitle());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            setSpinner(true);
            Task<Boolean> deleteTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return BookDAO.deleteBook(selectedBook.getBookId());
                }
            };
            deleteTask.setOnSucceeded(ev -> {
                setSpinner(false);
                if (deleteTask.getValue()) {
                    showSuccess("Book deleted successfully.");
                    loadBooksInBackground();
                } else {
                    showAlert("Error", "Failed to delete book. It might be referenced in transactions.");
                }
            });
            deleteTask.setOnFailed(ev -> {
                setSpinner(false);
                showAlert("Error", "Failed to delete book. (It may be referenced in transactions.)");
            });
            new Thread(deleteTask).start();
        }
    }

    @FXML
    private void handleReturnBook() {
        BorrowedBook selectedBorrowed = borrowedBooksTable.getSelectionModel().getSelectedItem();
        if (selectedBorrowed == null) {
            showAlert("No Selection", "Please select a borrowed book to return.");
            return;
        }
        returnBookButton.setDisable(true);
        setSpinner(true);

        Task<Boolean> returnTask = new Task<>() {
            @Override
            protected Boolean call() {
                return TransactionDAO.returnBookTransaction(selectedBorrowed.getTransactionId());
            }
        };
        returnTask.setOnSucceeded(ev -> {
            setSpinner(false);
            boolean success = returnTask.getValue();
            showAlert(success ? "Success" : "Error", success ? "Book returned successfully!" : "Failed to return book.");
            returnBookButton.setDisable(false);
            loadBooksInBackground();
            loadBorrowedBooksInBackground();
        });
        returnTask.setOnFailed(ev -> {
            setSpinner(false);
            showAlert("Error", "Failed to return book.");
            returnBookButton.setDisable(false);
            loadBooksInBackground();
            loadBorrowedBooksInBackground();
        });
        new Thread(returnTask).start();
    }

    private void showAlert(String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Notice");
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private void setSpinner(boolean visible) {
        if (progressIndicator != null) {
            Platform.runLater(() -> progressIndicator.setVisible(visible));
        }
    }

    @FXML
    private void handleBack(MouseEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/LoginLibrarian.fxml"));
        Parent page = loader.load();
        Stage stage = new Stage();
        stage.setScene(new Scene(page));
        stage.show();
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
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
}
