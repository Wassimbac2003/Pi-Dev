package com.healthtrack.entities;

public class disponibilite {
    private int id;
    private String date_dispo, hdebut, h_fin, statut;
    private int nbr_h, med_id;

    public disponibilite() {
    }

    public disponibilite(String date_dispo, String hdebut, String h_fin, String statut, int nbr_h, int med_id) {
        this.date_dispo = date_dispo;
        this.hdebut = hdebut;
        this.h_fin = h_fin;
        this.statut = statut;
        this.nbr_h = nbr_h;
        this.med_id = med_id;
    }

    public disponibilite(int id, String date_dispo, String hdebut, String h_fin, String statut, int nbr_h, int med_id) {
        this.id = id;
        this.date_dispo = date_dispo;
        this.hdebut = hdebut;
        this.h_fin = h_fin;
        this.statut = statut;
        this.nbr_h = nbr_h;
        this.med_id = med_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDate_dispo() { return date_dispo; }
    public void setDate_dispo(String date_dispo) { this.date_dispo = date_dispo; }

    public String getHdebut() { return hdebut; }
    public void setHdebut(String hdebut) { this.hdebut = hdebut; }

    public String getH_fin() { return h_fin; }
    public void setH_fin(String h_fin) { this.h_fin = h_fin; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public int getNbr_h() { return nbr_h; }
    public void setNbr_h(int nbr_h) { this.nbr_h = nbr_h; }

    public int getMed_id() { return med_id; }
    public void setMed_id(int med_id) { this.med_id = med_id; }

    @Override
    public String toString() {
        return "disponibilite{" +
                "id=" + id +
                ", date_dispo='" + date_dispo + '\'' +
                ", hdebut='" + hdebut + '\'' +
                ", h_fin='" + h_fin + '\'' +
                ", statut='" + statut + '\'' +
                ", nbr_h=" + nbr_h +
                ", med_id=" + med_id +
                '}';
    }
}