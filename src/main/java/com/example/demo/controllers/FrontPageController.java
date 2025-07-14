package com.example.demo.controllers;

import com.example.demo.controllers.models.Book;
import com.example.demo.controllers.models.User;
import com.example.demo.controllers.models.BorrowedBook;
import com.example.demo.controllers.utils.BookDAO;
import com.example.demo.controllers.utils.UserDAO;
import com.example.demo.controllers.utils.BorrowedBookDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FrontPageController {

    @FXML
    private ListView<String> recentBooksList;

    @FXML
    private ListView<String> recentUsersList;

    @FXML
    private ListView<String> nearOverdueList;
    @FXML private TextField searchField;
    @FXML private Button searchButton;

    // --- Dashboard Data Population ---
    @FXML
    private void initialize() {
        loadRecentBooks();
        loadRecentUsers();
        loadNearOverdueBooks();

        ContextMenu suggestionsMenu = new ContextMenu();
        final boolean[] menuNavigation = {false};
        java.util.function.Consumer<Book> showDetails = book -> {
            suggestionsMenu.hide();
            if (book != null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Book Details");
                alert.setHeaderText(book.getTitle() + " by " + book.getAuthor());
                alert.setContentText("ISBN: " + book.getIsbn()
                        + "\nStatus: " + book.getStatus()
                        + "\nCopies: " + book.getCopies());
                alert.showAndWait();
                searchField.clear(); // Clear after details shown
            }
        };

        searchField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.trim().length() < 3) {
                suggestionsMenu.hide();
                return;
            }
            menuNavigation[0] = false; // Reset when typing
            suggestionsMenu.getItems().clear();
            MenuItem loading = new MenuItem("Searching...");
            loading.setDisable(true);
            suggestionsMenu.getItems().add(loading);
            suggestionsMenu.show(searchField, javafx.geometry.Side.BOTTOM, 0, 0);
            // KEY FIX: Use runLater so field keeps focus even on first use!
            javafx.application.Platform.runLater(() -> searchField.requestFocus());

            javafx.concurrent.Task<List<Book>> searchTask = new javafx.concurrent.Task<>() {
                @Override
                protected List<Book> call() {
                    return BookDAO.searchBooks(newText.trim());
                }
            };

            searchTask.setOnSucceeded(evt -> {
                List<Book> matches = searchTask.getValue();
                suggestionsMenu.getItems().clear();
                int maxSuggestions = 10;
                int count = 0;
                for (Book book : matches) {
                    if (++count > maxSuggestions) break;
                    MenuItem item = new MenuItem(book.getTitle() + " by " + book.getAuthor());
                    item.setOnAction(e -> showDetails.accept(book));
                    suggestionsMenu.getItems().add(item);
                }
                if (matches.isEmpty()) {
                    MenuItem none = new MenuItem("No results found");
                    none.setDisable(true);
                    suggestionsMenu.getItems().add(none);
                }
                if (!matches.isEmpty()) {
                    suggestionsMenu.show(searchField, javafx.geometry.Side.BOTTOM, 0, 0);
                    javafx.application.Platform.runLater(() -> searchField.requestFocus());
                } else {
                    suggestionsMenu.hide();
                }
            });

            searchTask.setOnFailed(evt -> suggestionsMenu.hide());
            new Thread(searchTask).start();
        });

        searchField.setOnKeyPressed(e -> {
            if (suggestionsMenu.isShowing()) {
                if (e.getCode() == KeyCode.ESCAPE) {
                    suggestionsMenu.hide();
                    e.consume();
                }
                if (e.getCode() == KeyCode.UP || e.getCode() == KeyCode.DOWN) {
                    menuNavigation[0] = true;
                }
                if (e.getCode() == KeyCode.SPACE) {
                    e.consume();
                }
                if (e.getCode() == KeyCode.ENTER) {
                    if (menuNavigation[0]) {
                        MenuItem selected = suggestionsMenu.getItems().stream()
                                .filter(item -> item.getStyleClass().contains("focused"))
                                .findFirst().orElse(null);
                        if (selected != null && !selected.isDisable()) {
                            selected.fire();
                        }
                    }
                    e.consume();
                }
            }
        });

        searchField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) suggestionsMenu.hide();
        });
    }

    private void loadRecentBooks() {
        List<Book> books = BookDAO.getRecentBooks(5); // Get latest 5 books
        ObservableList<String> items = FXCollections.observableArrayList();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Book book : books) {
            String displayDate = "";
            Timestamp ts = book.getDateAdded();
            if (ts != null) {
                displayDate = ts.toLocalDateTime().format(formatter);
            }
            items.add(book.getTitle() + " (" + displayDate + ")");
        }
        recentBooksList.setItems(items);
    }

    private void loadRecentUsers() {
        List<User> users = UserDAO.getRecentUsers(5); // Get latest 5 users
        ObservableList<String> items = FXCollections.observableArrayList();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (User user : users) {
            String displayDate = "";
            Timestamp ts = user.getDateRegistered();
            if (ts != null) {
                displayDate = ts.toLocalDateTime().format(formatter);
            }
            items.add(user.getFullName() + " (" + displayDate + ")");
        }
        recentUsersList.setItems(items);
    }

    private void loadNearOverdueBooks() {
        List<BorrowedBook> books = BorrowedBookDAO.getNearOverdueBooks(5); // 5 books near overdue
        ObservableList<String> items = FXCollections.observableArrayList();
        for (BorrowedBook book : books) {
            items.add(book.getTitle() + " (due " + book.getDueDate() + ")");
        }
        nearOverdueList.setItems(items);
    }

    // --- Existing Methods ---

    @FXML
    private void handleProceed(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/SelectionRole.fxml"));
        Parent selectionPanel = loader.load();
        SelectionRoleController controller = loader.getController();
        Stage mainStage = (Stage) ((Window) ((javafx.scene.Node) event.getSource()).getScene().getWindow());
        controller.setMainStage(mainStage);
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setScene(new Scene(selectionPanel));
        dialog.centerOnScreen();
        dialog.showAndWait();
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("About this Library System");
        alert.setContentText("This is a demo library management system for students and librarians.");
        alert.showAndWait();
    }

    @FXML
    private void handleHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("FAQs for users");
        alert.setContentText("Forgot password?: Contact the Librarian: 09488778315"
                + "\nHow to borrow a book: Go to the dashboard, select a book, and click 'Borrow'"
                + "\nHow to return a book: Go to 'Return' and select your borrowed book");
        alert.showAndWait();
    }

    @FXML
    private void handleReference() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("References");
        alert.setHeaderText("ChatGPT IDEAS MINI PROJECT USING JAVA/JAVAFX");
        alert.setContentText("JAVA/JAVAFX/SCENE BUILDER/POSTGRESQL");
        alert.showAndWait();
    }

    @FXML
    private void handleSearchBooks() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Search");
            alert.setHeaderText("No keyword entered");
            alert.setContentText("Please enter a book title, author, or ISBN.");
            alert.showAndWait();
            return;
        }
        // Search for books using your BookDAO
        List<Book> results = BookDAO.searchBooks(keyword);
        if (results.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Search");
            alert.setHeaderText("No results found");
            alert.setContentText("No books match your search.");
            alert.showAndWait();
        } else {
            StringBuilder sb = new StringBuilder();
            for (Book book : results) {
                sb.append("- ").append(book.getTitle()).append(" by ").append(book.getAuthor())
                        .append(" (").append(book.getStatus()).append(")\n");
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Search Results");
            alert.setHeaderText("Books matching: " + keyword);
            alert.setContentText(sb.toString());
            alert.showAndWait();
        }
    }

}
