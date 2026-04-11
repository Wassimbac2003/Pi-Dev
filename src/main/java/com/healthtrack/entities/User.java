package com.healthtrack.entities;

public class User {
    private int id;
    private String email;
    private String password;
    private String nom;
    private String prenom;
    private String adresse;
    private String telephone;
    private String skillsProfile;
    private String interestsProfile;
    private String availabilityProfile;
    private String preferredCity;
    private Integer actionRadiusKm;
    private Double latitude;
    private Double longitude;
    private String recommendationWeights;
    private String role;
    private String profilePicture;
    private String diplomaDocument;
    private String idCardDocument;
    private boolean verified;
    private String verificationStatus;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getSkillsProfile() {
        return skillsProfile;
    }

    public void setSkillsProfile(String skillsProfile) {
        this.skillsProfile = skillsProfile;
    }

    public String getInterestsProfile() {
        return interestsProfile;
    }

    public void setInterestsProfile(String interestsProfile) {
        this.interestsProfile = interestsProfile;
    }

    public String getAvailabilityProfile() {
        return availabilityProfile;
    }

    public void setAvailabilityProfile(String availabilityProfile) {
        this.availabilityProfile = availabilityProfile;
    }

    public String getPreferredCity() {
        return preferredCity;
    }

    public void setPreferredCity(String preferredCity) {
        this.preferredCity = preferredCity;
    }

    public Integer getActionRadiusKm() {
        return actionRadiusKm;
    }

    public void setActionRadiusKm(Integer actionRadiusKm) {
        this.actionRadiusKm = actionRadiusKm;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getRecommendationWeights() {
        return recommendationWeights;
    }

    public void setRecommendationWeights(String recommendationWeights) {
        this.recommendationWeights = recommendationWeights;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getDiplomaDocument() {
        return diplomaDocument;
    }

    public void setDiplomaDocument(String diplomaDocument) {
        this.diplomaDocument = diplomaDocument;
    }

    public String getIdCardDocument() {
        return idCardDocument;
    }

    public void setIdCardDocument(String idCardDocument) {
        this.idCardDocument = idCardDocument;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getFullName() {
        return safe(nom) + (prenom == null || prenom.isBlank() ? "" : " " + prenom.trim());
    }

    public boolean isAdmin() {
        return "ROLE_ADMIN".equalsIgnoreCase(role);
    }

    public boolean isDoctor() {
        return "ROLE_MEDECIN".equalsIgnoreCase(role);
    }

    public boolean isPatient() {
        return "ROLE_PATIENT".equalsIgnoreCase(role);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}