package com.healthtrack.util;

import com.healthtrack.entities.User;

public final class SessionContext {
    private static User currentUser;

    private SessionContext() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }

    public static boolean isAuthenticated() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public static int getCurrentUserId() {
        return currentUser == null ? 0 : currentUser.getId();
    }
}