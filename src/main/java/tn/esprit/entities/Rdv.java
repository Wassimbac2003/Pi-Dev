package tn.esprit.entities;

import java.time.LocalDate;

public class Rdv {
    private int id;
    private LocalDate date;
    private String motif;
    private String statut;
    private Integer patientId;

    public Rdv() {
    }

    public Rdv(int id, LocalDate date, String motif, String statut, Integer patientId) {
        this.id = id;
        this.date = date;
        this.motif = motif;
        this.statut = statut;
        this.patientId = patientId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Integer getPatientId() {
        return patientId;
    }

    public void setPatientId(Integer patientId) {
        this.patientId = patientId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (date != null) {
            sb.append(date);
        }
        if (motif != null && !motif.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(motif.trim());
        }
        if (statut != null && !statut.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(statut.trim());
        }
        return sb.length() > 0 ? sb.toString() : "Rendez-vous";
    }
}
