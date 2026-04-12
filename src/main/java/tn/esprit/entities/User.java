package tn.esprit.entities;

public class User {
    private int id;
    private String email;
    private String nom;
    private String prenom;
    private String role;

    public User() {
    }

    public User(int id, String email, String nom, String prenom, String role) {
        this.id = id;
        this.email = email;
        this.nom = nom;
        this.prenom = prenom;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isMedecin() {
        return role != null && role.toUpperCase().contains("MEDECIN");
    }

    public boolean isPatientLike() {
        return role != null && (role.toUpperCase().contains("PATIENT")
                || role.toUpperCase().contains("USER")
                || role.toUpperCase().contains("ROLE_USER"));
    }

    @Override
    public String toString() {
        return prenom + " " + nom + " (" + email + ") — " + role;
    }
}
