package com.example.demo.controllers;

import com.example.demo.controllers.models.BorrowedBook;
import com.example.demo.controllers.utils.TransactionDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.util.List;

public class MyBorrowedBooksDialogController {

    @FXML private TableView<BorrowedBook> borrowedBooksTable;
    @FXML private TableColumn<BorrowedBook, String> titleColumn;
    @FXML private TableColumn<BorrowedBook, String> borrowDateColumn;
    @FXML private TableColumn<BorrowedBook, String> dueDateColumn;
    @FXML private TableColumn<BorrowedBook, String> statusColumn;
    @FXML private TableColumn<BorrowedBook, Void> actionColumn;

    private int userId;

    // Call this before showing the dialog!
    public void setUserId(int userId) {
        this.userId = userId;
        loadBorrowedBooks();
    }

    private void loadBorrowedBooks() {
        List<BorrowedBook> books = TransactionDAO.getBorrowedBooksByUser(userId);
        ObservableList<BorrowedBook> observableBooks = FXCollections.observableArrayList(books);

        titleColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getBookTitle()));
        borrowDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getBorrowDate()));
        dueDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDueDate()));
        statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus()));

        // Action Button: Return Book
        actionColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<BorrowedBook, Void> call(final TableColumn<BorrowedBook, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button("Return");
                    {
                        btn.setStyle("-fx-background-color: #ff6f61; -fx-text-fill: white; -fx-font-size: 11px;");
                        btn.setOnAction(event -> {
                            BorrowedBook data = getTableView().getItems().get(getIndex());
                            boolean ok = TransactionDAO.returnBookByTransactionId(data.getTransactionId());
                            Alert alert = new Alert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                            alert.setHeaderText(null);
                            alert.setContentText(ok ? "Book returned!" : "Return failed.");
                            alert.showAndWait();
                            loadBorrowedBooks();
                        });
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || !"Borrowed".equalsIgnoreCase(getTableView().getItems().get(getIndex()).getStatus())) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
            }
        });

        borrowedBooksTable.setItems(observableBooks);
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) borrowedBooksTable.getScene().getWindow();
        stage.close();
    }
}
