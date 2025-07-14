package com.example.demo.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class SelectionRoleController {

    private Stage mainStage;
    public void setMainStage(Stage stage) {
        this.mainStage = stage;
    }

    @FXML
    private void handleStudent(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/LoginStudent.fxml"));
            Parent loginPage = loader.load();

            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(loginPage));
            loginStage.setTitle("Student Login");
            loginStage.show();

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            if (mainStage != null) {
                mainStage.close();
            }
        } catch (IOException e) {
            showAlert("Failed to load Student Login", e.getMessage());
        } catch (Exception e) {
            showAlert("Unexpected Error", e.getMessage());
        }
    }

    @FXML
    private void handleLibrarian(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/LoginLibrarian.fxml"));
            Parent loginPage = loader.load();

            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(loginPage));
            loginStage.setTitle("Librarian Login");
            loginStage.show();

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            if (mainStage != null) {
                mainStage.close();
            }
        } catch (IOException e) {
            showAlert("Failed to load Librarian Login", e.getMessage());
        } catch (Exception e) {
            showAlert("Unexpected Error", e.getMessage());
        }
    }

    private void showAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Navigation Error");
        alert.setHeaderText(header);
        alert.setContentText(content != null ? content : "Please try again.");
        alert.showAndWait();
    }
}
