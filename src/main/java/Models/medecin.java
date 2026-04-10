package Models;

public class medecin {
    private int id;
    private String nom, prenom, specialite, type, photo;
    private int disponible, user_id;

    public medecin() {
    }

    public medecin(String nom, String prenom, String specialite, String type, int disponible, String photo, int user_id) {
        this.nom = nom;
        this.prenom = prenom;
        this.specialite = specialite;
        this.type = type;
        this.disponible = disponible;
        this.photo = photo;
        this.user_id = user_id;
    }

    public medecin(int id, String nom, String prenom, String specialite, String type, int disponible, String photo, int user_id) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.specialite = specialite;
        this.type = type;
        this.disponible = disponible;
        this.photo = photo;
        this.user_id = user_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getDisponible() { return disponible; }
    public void setDisponible(int disponible) { this.disponible = disponible; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    @Override
    public String toString() {
        return "medecin{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", specialite='" + specialite + '\'' +
                ", type='" + type + '\'' +
                ", disponible=" + disponible +
                ", photo='" + photo + '\'' +
                ", user_id=" + user_id +
                '}';
    }
}