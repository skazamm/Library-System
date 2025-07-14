package com.example.demo.controllers.models;

public class BorrowedBook {
    private int transactionId;
    private String bookTitle;
    private String borrowerName;
    private String borrowDate;
    private String dueDate;
    private String status;

    // Constructor
    public BorrowedBook(int transactionId, String bookTitle, String borrowerName, String borrowDate, String dueDate, String status) {
        this.transactionId = transactionId;
        this.bookTitle = bookTitle;
        this.borrowerName = borrowerName;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = status;
    }

    // Getters and setters
    public int getTransactionId() {
        return transactionId;
    }
    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public String getBookTitle() {
        return bookTitle;
    }
    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBorrowerName() {
        return borrowerName;
    }
    public void setBorrowerName(String borrowerName) {
        this.borrowerName = borrowerName;
    }

    public String getBorrowDate() {
        return borrowDate;
    }
    public void setBorrowDate(String borrowDate) {
        this.borrowDate = borrowDate;
    }

    public String getDueDate() {
        return dueDate;
    }
    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    // For dashboard compatibility:
    public String getTitle() {
        return bookTitle;
    }
}
