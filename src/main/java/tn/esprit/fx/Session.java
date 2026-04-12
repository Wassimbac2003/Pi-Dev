package tn.esprit.fx;

import tn.esprit.entities.User;

public final class Session {

    public enum Portal {
        ADMIN,
        MEDECIN,
        PATIENT
    }

    private static Portal portal;
    private static User currentUser;

    private Session() {
    }

    public static void setAdmin() {
        portal = Portal.ADMIN;
        currentUser = null;
    }

    public static void setMedecin(User u) {
        portal = Portal.MEDECIN;
        currentUser = u;
    }

    public static void setPatient(User u) {
        portal = Portal.PATIENT;
        currentUser = u;
    }

    public static Portal getPortal() {
        return portal;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : 0;
    }

    public static void clear() {
        portal = null;
        currentUser = null;
    }
}
