package tn.esprit.entities;

import java.sql.Date;
import java.time.LocalDate;

public class Medicament {
    private int id;
    private String nomMedicament;
    private String categorie;
    private String dosage;
    private String forme;
    private LocalDate dateExpiration;

    public Medicament() {
    }

    public Medicament(int id, String nomMedicament, String categorie, String dosage,
                      String forme, LocalDate dateExpiration) {
        this.id = id;
        this.nomMedicament = nomMedicament;
        this.categorie = categorie;
        this.dosage = dosage;
        this.forme = forme;
        this.dateExpiration = dateExpiration;
    }

    public Medicament(String nomMedicament, String categorie, String dosage, String forme, Date dateExpiration) {
        this(0, nomMedicament, categorie, dosage, forme, dateExpiration.toLocalDate());
    }

    public Medicament(int id, String nomMedicament, String categorie, String dosage, String forme, Date dateExpiration) {
        this(id, nomMedicament, categorie, dosage, forme, dateExpiration.toLocalDate());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNomMedicament() {
        return nomMedicament;
    }

    public void setNomMedicament(String nomMedicament) {
        this.nomMedicament = nomMedicament;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getForme() {
        return forme;
    }

    public void setForme(String forme) {
        this.forme = forme;
    }

    public LocalDate getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDate dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    @Override
    public String toString() {
        return "Medicament{" +
                "id=" + id +
                ", nomMedicament='" + nomMedicament + '\'' +
                ", categorie='" + categorie + '\'' +
                ", dosage='" + dosage + '\'' +
                ", forme='" + forme + '\'' +
                ", dateExpiration=" + dateExpiration +
                '}';
    }
}
