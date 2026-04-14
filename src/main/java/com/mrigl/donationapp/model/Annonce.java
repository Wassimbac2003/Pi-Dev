package com.mrigl.donationapp.model;

import java.time.LocalDate;
import java.util.Objects;

public class Annonce {

    private Long id;
    private String titreAnnonce;
    private String description;
    private LocalDate datePublication;
    private String urgence;
    private String etatAnnonce;

    public Annonce() {
        this.datePublication = LocalDate.now();
    }

    public Annonce(Long id, String titreAnnonce, String description, LocalDate datePublication,
                   String urgence, String etatAnnonce) {
        this.id = id;
        this.titreAnnonce = titreAnnonce;
        this.description = description;
        this.datePublication = datePublication;
        this.urgence = urgence;
        this.etatAnnonce = etatAnnonce;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitreAnnonce() {
        return titreAnnonce;
    }

    public void setTitreAnnonce(String titreAnnonce) {
        this.titreAnnonce = titreAnnonce;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(LocalDate datePublication) {
        this.datePublication = datePublication;
    }

    public String getUrgence() {
        return urgence;
    }

    public void setUrgence(String urgence) {
        this.urgence = urgence;
    }

    public String getEtatAnnonce() {
        return etatAnnonce;
    }

    public void setEtatAnnonce(String etatAnnonce) {
        this.etatAnnonce = etatAnnonce;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Annonce annonce)) {
            return false;
        }
        return Objects.equals(id, annonce.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return titreAnnonce == null || titreAnnonce.isBlank() ? "Annonce" : titreAnnonce;
    }
}
