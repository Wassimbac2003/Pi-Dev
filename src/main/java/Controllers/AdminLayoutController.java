package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AdminLayoutController {

    @FXML private Label pageTitle, lblDateAujourdhui;
    @FXML private StackPane contentArea;
    @FXML private Button btnAdminRdv;

    @FXML
    public void initialize() {
        lblDateAujourdhui.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        chargerContenu("/AdminRdv.fxml");

        btnAdminRdv.setOnAction(e -> chargerContenu("/AdminRdv.fxml"));
    }

    public void chargerContenu(String fxml) {
        try {
            Node content = FXMLLoader.load(getClass().getResource(fxml));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            System.err.println("Erreur chargement " + fxml + " :");
            e.printStackTrace();
        }
    }
}
