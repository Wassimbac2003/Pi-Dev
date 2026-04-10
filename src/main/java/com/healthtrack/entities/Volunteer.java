package com.healthtrack.entities;

import com.healthtrack.tools.JsonUtil;

public class Volunteer {
    private int id;
    private String motivation;
    private String disponibilitesJson;
    private String telephone;
    private String statut;
    private int userId;
    private int missionId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public String getDisponibilitesJson() {
        return disponibilitesJson;
    }

    public void setDisponibilitesJson(String disponibilitesJson) {
        this.disponibilitesJson = disponibilitesJson;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getMissionId() {
        return missionId;
    }

    public void setMissionId(int missionId) {
        this.missionId = missionId;
    }

    public String getDisponibilitesCsv() {
        return JsonUtil.toCsv(disponibilitesJson);
    }
}
