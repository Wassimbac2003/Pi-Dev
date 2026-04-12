package tn.esprit.entities;

import java.sql.Date;
import java.time.LocalDateTime;

public class Ordonnance {
    private int id;
    private String posologie;
    private String frequence;
    private int dureeTraitement;
    private LocalDateTime dateOrdonnance;
    private String scanToken;
    private int idUId;
    /** Ancien lien RDV ; peut être nul après migration. */
    private Integer idRdvId;
    private int idFicheId;
    private Integer medecinUserId;

    public Ordonnance() {
    }

    public Ordonnance(int id, String posologie, String frequence, int dureeTraitement,
                      LocalDateTime dateOrdonnance, String scanToken, int idUId, Integer idRdvId,
                      int idFicheId, Integer medecinUserId) {
        this.id = id;
        this.posologie = posologie;
        this.frequence = frequence;
        this.dureeTraitement = dureeTraitement;
        this.dateOrdonnance = dateOrdonnance;
        this.scanToken = scanToken;
        this.idUId = idUId;
        this.idRdvId = idRdvId;
        this.idFicheId = idFicheId;
        this.medecinUserId = medecinUserId;
    }

    /** Insert : date à minuit, {@code scanToken} nul. */
    public Ordonnance(String posologie, String frequence, int dureeTraitement, Date date,
                      int idUId, Integer idRdvId, int idFicheId) {
        this.posologie = posologie;
        this.frequence = frequence;
        this.dureeTraitement = dureeTraitement;
        this.dateOrdonnance = date.toLocalDate().atStartOfDay();
        this.scanToken = null;
        this.idUId = idUId;
        this.idRdvId = idRdvId;
        this.idFicheId = idFicheId;
    }

    /** Update : {@code scanToken} nul. */
    public Ordonnance(int id, String posologie, String frequence, int dureeTraitement, Date date,
                      int idUId, Integer idRdvId, int idFicheId) {
        this.id = id;
        this.posologie = posologie;
        this.frequence = frequence;
        this.dureeTraitement = dureeTraitement;
        this.dateOrdonnance = date.toLocalDate().atStartOfDay();
        this.scanToken = null;
        this.idUId = idUId;
        this.idRdvId = idRdvId;
        this.idFicheId = idFicheId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPosologie() {
        return posologie;
    }

    public void setPosologie(String posologie) {
        this.posologie = posologie;
    }

    public String getFrequence() {
        return frequence;
    }

    public void setFrequence(String frequence) {
        this.frequence = frequence;
    }

    public int getDureeTraitement() {
        return dureeTraitement;
    }

    public void setDureeTraitement(int dureeTraitement) {
        this.dureeTraitement = dureeTraitement;
    }

    public LocalDateTime getDateOrdonnance() {
        return dateOrdonnance;
    }

    public void setDateOrdonnance(LocalDateTime dateOrdonnance) {
        this.dateOrdonnance = dateOrdonnance;
    }

    public String getScanToken() {
        return scanToken;
    }

    public void setScanToken(String scanToken) {
        this.scanToken = scanToken;
    }

    public int getIdUId() {
        return idUId;
    }

    public void setIdUId(int idUId) {
        this.idUId = idUId;
    }

    public Integer getIdRdvId() {
        return idRdvId;
    }

    public void setIdRdvId(Integer idRdvId) {
        this.idRdvId = idRdvId;
    }

    public int getIdFicheId() {
        return idFicheId;
    }

    public void setIdFicheId(int idFicheId) {
        this.idFicheId = idFicheId;
    }

    public Integer getMedecinUserId() {
        return medecinUserId;
    }

    public void setMedecinUserId(Integer medecinUserId) {
        this.medecinUserId = medecinUserId;
    }

    @Override
    public String toString() {
        return "Ordonnance{" +
                "id=" + id +
                ", posologie='" + posologie + '\'' +
                ", frequence='" + frequence + '\'' +
                ", dureeTraitement=" + dureeTraitement +
                ", dateOrdonnance=" + dateOrdonnance +
                ", scanToken='" + scanToken + '\'' +
                ", idUId=" + idUId +
                ", idRdvId=" + idRdvId +
                ", idFicheId=" + idFicheId +
                ", medecinUserId=" + medecinUserId +
                '}';
    }
}
