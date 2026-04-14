package com.mrigl.donationapp.model;

import java.time.LocalDate;
import java.util.Objects;

public class Donation {

    private Long id;
    private String typeDon;
    private Integer quantite;
    private LocalDate dateDonation;
    private String statut;
    private Long annonceId;

    public Donation() {
    }

    public Donation(Long id, String typeDon, Integer quantite, LocalDate dateDonation,
                    String statut, Long annonceId) {
        this.id = id;
        this.typeDon = typeDon;
        this.quantite = quantite;
        this.dateDonation = dateDonation;
        this.statut = statut;
        this.annonceId = annonceId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTypeDon() {
        return typeDon;
    }

    public void setTypeDon(String typeDon) {
        this.typeDon = typeDon;
    }

    public Integer getQuantite() {
        return quantite;
    }

    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
    }

    public LocalDate getDateDonation() {
        return dateDonation;
    }

    public void setDateDonation(LocalDate dateDonation) {
        this.dateDonation = dateDonation;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Long getAnnonceId() {
        return annonceId;
    }

    public void setAnnonceId(Long annonceId) {
        this.annonceId = annonceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Donation donation)) {
            return false;
        }
        return Objects.equals(id, donation.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
