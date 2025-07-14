package com.example.demo.controllers;

import com.example.demo.controllers.models.Book;
import com.example.demo.controllers.models.BorrowedBook;
import com.example.demo.controllers.models.User;
import com.example.demo.controllers.utils.BookDAO;
import com.example.demo.controllers.utils.TransactionDAO;
import javafx.animation.TranslateTransition;
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
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class StudentManagementController {

    @FXML private Button borrowBookButton;
    @FXML private Button returnBookButton;
    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, String> isbnColumn;
    @FXML private TableColumn<Book, String> statusColumn;
    @FXML private TableColumn<Book, Integer> copiesColumn;
    @FXML private TextField searchField;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label welcomeLabel;
    @FXML private Button logoutButton;
    // --- Drawer controls ---
    @FXML private Button drawerToggleButton;
    @FXML private AnchorPane drawerPane;
    @FXML private Button drawerCloseButton;

    @FXML private Button myBorrowedBooksButton;
    @FXML private Button notificationsButton;
    @FXML private Button userProfileButton;


    private final ObservableList<Book> bookList = FXCollections.observableArrayList();
    private boolean isDrawerOpen = false;
    private static final double DRAWER_WIDTH = 300; // As in FXML
    private static final double DRAWER_START_X = 914.0; // width of your scene (offscreen right)
    private static final double DRAWER_END_X = 694;   // visible (right-aligned in window of 914px wide)

    // In a real app, set this to the actual student’s user ID after login!
    private User currentUser;

    @FXML
    public void initialize() {
        // Set up table columns
        titleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));
        authorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAuthor()));
        isbnColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getIsbn()));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        copiesColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getCopies()).asObject());
        bookTable.setItems(bookList);

        // Hide spinner by default
        if (progressIndicator != null) progressIndicator.setVisible(false);

        // Initial load of all books (background)
        loadBooksInBackground();

        // Live search as you type
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> searchBooksInBackground(newV));
        }

        // Drawer initial setup: make sure it's offscreen right & invisible at start
        if (drawerPane != null) {
            drawerPane.setLayoutX(DRAWER_START_X);
            drawerPane.setTranslateX(0);           // start at rest
            drawerPane.setVisible(false);
        }
        // Attach handlers
        if (drawerToggleButton != null) {
            drawerToggleButton.setOnAction(e -> toggleDrawer());
        }
        if (drawerCloseButton != null) {
            drawerCloseButton.setOnAction(e -> toggleDrawer());
        }
    }

    // Add this public setter:
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null && user != null) {
            welcomeLabel.setText("Welcome, " + user.getName() + "!");
        }
    }

    private void setSpinner(boolean visible) {
        if (progressIndicator != null) {
            Platform.runLater(() -> progressIndicator.setVisible(visible));
        }
    }

    // Load all books (in background)
    private void loadBooksInBackground() {
        setSpinner(true);
        Task<List<Book>> loadBooksTask = new Task<>() {
            @Override
            protected List<Book> call() {
                return BookDAO.getAllBooks();
            }
        };
        loadBooksTask.setOnSucceeded(e -> {
            List<Book> result = loadBooksTask.getValue();
            bookList.setAll(result != null ? result : new ArrayList<>());
            setSpinner(false);
        });
        loadBooksTask.setOnFailed(e -> {
            bookList.clear();
            showAlert("Error", "Failed to load books.");
            setSpinner(false);
        });
        new Thread(loadBooksTask).start();
    }

    // Search books (in background)
    private void searchBooksInBackground(String keyword) {
        setSpinner(true);
        Task<List<Book>> searchTask = new Task<>() {
            @Override
            protected List<Book> call() {
                if (keyword == null || keyword.isBlank()) {
                    return BookDAO.getAllBooks();
                }
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

    @FXML
    private void handleBorrowBook() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert("No Selection", "Please select a book to borrow.");
            return;
        }
        if (selectedBook.getCopies() <= 0 || !"Available".equalsIgnoreCase(selectedBook.getStatus())) {
            showAlert("Unavailable", "No copies available to borrow.");
            return;
        }
        borrowBookButton.setDisable(true);
        setSpinner(true);

        Task<Boolean> borrowTask = new Task<>() {
            @Override
            protected Boolean call() {
                return TransactionDAO.borrowBook(currentUser.getUserId(), selectedBook.getBookId());
            }
        };

        borrowTask.setOnSucceeded(e -> {
            boolean success = borrowTask.getValue();
            showAlert(success ? "Success" : "Error", success ? "Book borrowed successfully!" : "Failed to borrow book.");
            borrowBookButton.setDisable(false);
            setSpinner(false);
            loadBooksInBackground();
        });

        borrowTask.setOnFailed(e -> {
            showAlert("Error", "Failed to borrow book (exception).");
            borrowBookButton.setDisable(false);
            setSpinner(false);
            loadBooksInBackground();
        });

        new Thread(borrowTask).start();
    }

    @FXML
    private void handleReturnBook() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert("No Selection", "Please select a book to return.");
            return;
        }
        returnBookButton.setDisable(true);
        if (progressIndicator != null) progressIndicator.setVisible(true);

        Task<Boolean> returnTask = new Task<>() {
            @Override
            protected Boolean call() {
                try {
                    return TransactionDAO.returnBook(currentUser.getUserId(), selectedBook.getBookId());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        returnTask.setOnSucceeded(e -> {
            boolean success = returnTask.getValue();
            if (success) {
                showAlert("Success", "Book returned successfully!");
            } else {
                showAlert("Not Borrowed", "You have not borrowed this book, or it is already returned.");
            }
            returnBookButton.setDisable(false);
            if (progressIndicator != null) progressIndicator.setVisible(false);
            loadBooksInBackground();
        });

        returnTask.setOnFailed(e -> {
            showAlert("Error", "An error occurred while trying to return the book. Please try again.");
            returnBookButton.setDisable(false);
            if (progressIndicator != null) progressIndicator.setVisible(false);
            loadBooksInBackground();
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

    @FXML
    private void handleBack(MouseEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/LoginStudent.fxml"));
        Parent loginPage = loader.load();
        Stage stage = new Stage();
        stage.setScene(new Scene(loginPage));
        stage.show();
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
    }

    /*** --- Drawer Logic --- ***/
    @FXML
    private void toggleDrawer() {
        if (drawerPane == null) return;

        // Show drawer before animating
        drawerPane.setVisible(true);

        // If opening, move from translateX = 0 (offscreen) to -DRAWER_WIDTH (onscreen)
        // If closing, move from -DRAWER_WIDTH (onscreen) to 0 (offscreen)
        double toTranslateX = isDrawerOpen ? 0 : -DRAWER_WIDTH;

        TranslateTransition tt = new TranslateTransition(Duration.millis(300), drawerPane);
        tt.setToX(toTranslateX);

        tt.setOnFinished(e -> {
            isDrawerOpen = !isDrawerOpen;
            if (!isDrawerOpen) {
                drawerPane.setVisible(false);
            }
        });
        tt.play();
    }

    @FXML
    private void handleLogout() {
        // Optional: Confirm logout
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout Confirmation");
        alert.setHeaderText("Are you sure you want to log out?");
        alert.setContentText("You will be returned to the login page.");
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    // Load login page (edit path as needed)
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/LoginStudent.fxml"));
                    Parent loginRoot = loader.load();
                    Stage loginStage = new Stage();
                    loginStage.setScene(new Scene(loginRoot));
                    loginStage.setTitle("Student Login");
                    loginStage.show();

                    // Close current window
                    Stage currentStage = (Stage) logoutButton.getScene().getWindow();
                    currentStage.close();
                } catch (Exception e) {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Failed to load login page: " + e.getMessage());
                    error.showAndWait();
                }
            }
        });
    }

    @FXML
    private void handleMyBorrowedBooks() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/MyBorrowedBooksDialog.fxml"));
            Parent root = loader.load();
            MyBorrowedBooksDialogController controller = loader.getController();
            controller.setUserId(currentUser.getUserId()); // pass currently logged-in student

            Stage dialog = new Stage();
            dialog.setTitle("My Borrowed Books");
            dialog.setScene(new Scene(root));
            dialog.initOwner(borrowBookButton.getScene().getWindow()); // or your main stage
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to open dialog:\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleNotifications() {
        // Example: Find books due in 3 days or less for current user
        List<BorrowedBook> borrowedBooks = TransactionDAO.getBorrowedBooksByUser(currentUser.getUserId());
        List<String> notifications = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (BorrowedBook bb : borrowedBooks) {
            // Parse dueDate string to LocalDate
            LocalDate dueDate = null;
            try { dueDate = LocalDate.parse(bb.getDueDate()); } catch (Exception ignored) {}
            if (dueDate != null) {
                long daysLeft = ChronoUnit.DAYS.between(now, dueDate);
                if (daysLeft < 0 && "Overdue".equalsIgnoreCase(bb.getStatus())) {
                    notifications.add("OVERDUE: \"" + bb.getBookTitle() + "\" was due " + dueDate + "!");
                } else if (daysLeft >= 0 && daysLeft <= 3) {
                    notifications.add("REMINDER: \"" + bb.getBookTitle() + "\" is due in " + daysLeft + " day(s) (" + dueDate + ")");
                }
            }
        }
        if (notifications.isEmpty()) {
            notifications.add("No new notifications. All books are up to date!");
        }
        StringBuilder sb = new StringBuilder();
        for (String note : notifications) sb.append("- ").append(note).append("\n");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText("Notifications");
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }

    @FXML
    private void handleUserProfile() {
        if (currentUser == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(currentUser.getName()).append("\n")
                .append("Username: ").append(currentUser.getUsername()).append("\n")
                .append("Email: ").append(currentUser.getEmail()).append("\n")
                .append("Contact: ").append(currentUser.getContactNumber() != null ? currentUser.getContactNumber() : "-").append("\n")
                .append("Birthday: ").append(currentUser.getBirthday() != null ? currentUser.getBirthday() : "-").append("\n")
                .append("Role: ").append(currentUser.getRole()).append("\n")
                .append("Registered: ").append(currentUser.getDateRegistered() != null ? currentUser.getDateRegistered().toLocalDateTime().toLocalDate() : "-");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("User Profile");
        alert.setHeaderText("Profile Details");
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }

}
