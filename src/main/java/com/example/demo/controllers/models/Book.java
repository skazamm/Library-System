package com.example.demo.controllers.models;

import java.sql.Timestamp;

public class Book {
    private int bookId;
    private String title;
    private String author;
    private String isbn;
    private String status;
    private int copies;
    private Timestamp dateAdded; // Format: "YYYY-MM-DD" or whatever you use

    // Updated constructor including dateAdded
    public Book(int bookId, String title, String author, String isbn, String status, int copies, Timestamp dateAdded) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.status = status;
        this.copies = copies;
        this.dateAdded = dateAdded;
    }

    // --- Getters and setters ---

    public int getBookId() {
        return bookId;
    }
    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public int getCopies() {
        return copies;
    }
    public void setCopies(int copies) {
        this.copies = copies;
    }

    public Timestamp getDateAdded() {
        return dateAdded;
    }
    public void setDateAdded(Timestamp dateAdded) {
        this.dateAdded = dateAdded;
    }
}
