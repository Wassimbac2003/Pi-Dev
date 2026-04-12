package tn.esprit.fx;

import javafx.geometry.Pos;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Champ de formulaire + message d erreur et surlignage rouge.
 */
public final class FormFieldSlot {

    private final Region input;
    private final Label errorLabel;
    private final VBox root;

    public FormFieldSlot(Region input) {
        this(input, true);
    }

    /**
     * @param autoClear efface erreur à la saisie (désactiver pour un conteneur regroupant plusieurs contrôles).
     */
    public FormFieldSlot(Region input, boolean autoClear) {
        this.input = input;
        errorLabel = new Label();
        errorLabel.getStyleClass().add("form-field-error-text");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(440);
        errorLabel.setAlignment(Pos.CENTER_LEFT);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        root = new VBox(4, input, errorLabel);
        root.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(input, Priority.NEVER);

        if (autoClear) {
            if (input instanceof TextInputControl tic) {
                tic.textProperty().addListener((o, a, b) -> clear());
            } else if (input instanceof ComboBoxBase<?> cb) {
                cb.valueProperty().addListener((o, a, b) -> clear());
            } else if (input instanceof DatePicker dp) {
                dp.valueProperty().addListener((o, a, b) -> clear());
            } else if (input instanceof ListView<?> lv) {
                lv.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> clear());
            }
        }
    }

    public VBox getRoot() {
        return root;
    }

    public Region getInput() {
        return input;
    }

    public void setError(String message) {
        if (message == null || message.isBlank()) {
            clear();
            return;
        }
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        FormValidationStyles.markInvalid(input);
    }

    public void clear() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        FormValidationStyles.clearInvalid(input);
    }

    public void markRelatedInvalid(Region other) {
        FormValidationStyles.markInvalid(other);
    }

    public void clearRelated(Region other) {
        FormValidationStyles.clearInvalid(other);
    }
}
