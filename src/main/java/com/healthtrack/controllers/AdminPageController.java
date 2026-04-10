package com.healthtrack.controllers;

public interface AdminPageController {
    void setAdminMainController(AdminMainController adminMainController);

    default void onPageShown() {
        // Optional refresh hook.
    }
}
