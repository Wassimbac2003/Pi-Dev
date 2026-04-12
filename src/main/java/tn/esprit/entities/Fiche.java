package tn.esprit.entities;

import java.sql.Date;
import java.time.LocalDate;

public class Fiche {
    private int id;
    private double poids;
    private double taille;
    private String grpSanguin;
    private String allergie;
    private String maladieChronique;
    private String tension;
    private double glycemie;
    private LocalDate date;
    private String libelleMaladie;
    private String gravite;
    private String recommandation;
    private String symptomes;
    private int idUId;
    /** Médecin ayant rédigé la fiche (optionnel). */
    private Integer medecinUserId;

    public Fiche() {
    }

    public Fiche(int id, double poids, double taille, String grpSanguin, String allergie,
                 String maladieChronique, String tension, double glycemie, LocalDate date,
                 String libelleMaladie, String gravite, String recommandation, String symptomes,
                 int idUId) {
        this.id = id;
        this.poids = poids;
        this.taille = taille;
        this.grpSanguin = grpSanguin;
        this.allergie = allergie;
        this.maladieChronique = maladieChronique;
        this.tension = tension;
        this.glycemie = glycemie;
        this.date = date;
        this.libelleMaladie = libelleMaladie;
        this.gravite = gravite;
        this.recommandation = recommandation;
        this.symptomes = symptomes;
        this.idUId = idUId;
    }

    /** Insert : {@code symptomes} vide par défaut. */
    public Fiche(double poids, double taille, String grpSanguin, String allergie,
                 String maladieChronique, String tension, double glycemie, Date date,
                 String libelleMaladie, String gravite, String recommandation, int idUId) {
        this(0, poids, taille, grpSanguin, allergie, maladieChronique, tension, glycemie,
                date.toLocalDate(), libelleMaladie, gravite, recommandation, "", idUId);
    }

    /** Update : {@code symptomes} vide si non renseigné dans cet appel. */
    public Fiche(int id, double poids, double taille, String grpSanguin, String allergie,
                 String maladieChronique, String tension, double glycemie, Date date,
                 String libelleMaladie, String gravite, String recommandation, int idUId) {
        this(id, poids, taille, grpSanguin, allergie, maladieChronique, tension, glycemie,
                date.toLocalDate(), libelleMaladie, gravite, recommandation, "", idUId);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getPoids() {
        return poids;
    }

    public void setPoids(double poids) {
        this.poids = poids;
    }

    public double getTaille() {
        return taille;
    }

    public void setTaille(double taille) {
        this.taille = taille;
    }

    public String getGrpSanguin() {
        return grpSanguin;
    }

    public void setGrpSanguin(String grpSanguin) {
        this.grpSanguin = grpSanguin;
    }

    public String getAllergie() {
        return allergie;
    }

    public void setAllergie(String allergie) {
        this.allergie = allergie;
    }

    public String getMaladieChronique() {
        return maladieChronique;
    }

    public void setMaladieChronique(String maladieChronique) {
        this.maladieChronique = maladieChronique;
    }

    public String getTension() {
        return tension;
    }

    public void setTension(String tension) {
        this.tension = tension;
    }

    public double getGlycemie() {
        return glycemie;
    }

    public void setGlycemie(double glycemie) {
        this.glycemie = glycemie;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getLibelleMaladie() {
        return libelleMaladie;
    }

    public void setLibelleMaladie(String libelleMaladie) {
        this.libelleMaladie = libelleMaladie;
    }

    public String getGravite() {
        return gravite;
    }

    public void setGravite(String gravite) {
        this.gravite = gravite;
    }

    public String getRecommandation() {
        return recommandation;
    }

    public void setRecommandation(String recommandation) {
        this.recommandation = recommandation;
    }

    public String getSymptomes() {
        return symptomes;
    }

    public void setSymptomes(String symptomes) {
        this.symptomes = symptomes;
    }

    public int getIdUId() {
        return idUId;
    }

    public void setIdUId(int idUId) {
        this.idUId = idUId;
    }

    public Integer getMedecinUserId() {
        return medecinUserId;
    }

    public void setMedecinUserId(Integer medecinUserId) {
        this.medecinUserId = medecinUserId;
    }

    @Override
    public String toString() {
        return "Fiche{" +
                "id=" + id +
                ", poids=" + poids +
                ", taille=" + taille +
                ", grpSanguin='" + grpSanguin + '\'' +
                ", allergie='" + allergie + '\'' +
                ", maladieChronique='" + maladieChronique + '\'' +
                ", tension='" + tension + '\'' +
                ", glycemie=" + glycemie +
                ", date=" + date +
                ", libelleMaladie='" + libelleMaladie + '\'' +
                ", gravite='" + gravite + '\'' +
                ", recommandation='" + recommandation + '\'' +
                ", symptomes='" + symptomes + '\'' +
                ", idUId=" + idUId +
                ", medecinUserId=" + medecinUserId +
                '}';
    }
}
