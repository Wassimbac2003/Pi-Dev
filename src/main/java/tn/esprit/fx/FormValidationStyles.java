package tn.esprit.fx;

import javafx.scene.layout.Region;

public final class FormValidationStyles {

    public static final String INVALID_CONTROL = "form-control-invalid";
    public static final String INVALID_SECTION = "form-section-invalid";

    private FormValidationStyles() {
    }

    public static void markInvalid(Region node) {
        if (node != null && !node.getStyleClass().contains(INVALID_CONTROL)) {
            node.getStyleClass().add(INVALID_CONTROL);
        }
    }

    public static void clearInvalid(Region node) {
        if (node != null) {
            node.getStyleClass().remove(INVALID_CONTROL);
        }
    }

    public static void markSection(Region section, boolean invalid) {
        if (section == null) {
            return;
        }
        if (invalid) {
            if (!section.getStyleClass().contains(INVALID_SECTION)) {
                section.getStyleClass().add(INVALID_SECTION);
            }
        } else {
            section.getStyleClass().remove(INVALID_SECTION);
        }
    }
}
