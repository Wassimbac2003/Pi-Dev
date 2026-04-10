package com.healthtrack.controllers;

public interface PageController {
    void setMainController(MainController mainController);

    default void onPageShown() {
        // Optional hook for controllers that refresh data when displayed.
    }
}
