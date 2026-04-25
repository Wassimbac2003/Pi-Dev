package Models;

public class rdv {
    private int id;
    private String date, hdebut, hfin, statut, motif, medecin, message;
    private int patient_id, medecin_user_id;
    private Integer feedbackNote;
    private String feedbackTags;
    private String feedbackCommentaire;
    private String feedbackDate;

    public rdv() {
    }

    public rdv(String date, String hdebut, String hfin, String statut, String motif, String medecin, String message, int patient_id, int medecin_user_id) {
        this.date = date;
        this.hdebut = hdebut;
        this.hfin = hfin;
        this.statut = statut;
        this.motif = motif;
        this.medecin = medecin;
        this.message = message;
        this.patient_id = patient_id;
        this.medecin_user_id = medecin_user_id;
    }

    public rdv(int id, String date, String hdebut, String hfin, String statut, String motif, String medecin, String message, int patient_id, int medecin_user_id) {
        this.id = id;
        this.date = date;
        this.hdebut = hdebut;
        this.hfin = hfin;
        this.statut = statut;
        this.motif = motif;
        this.medecin = medecin;
        this.message = message;
        this.patient_id = patient_id;
        this.medecin_user_id = medecin_user_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getHdebut() { return hdebut; }
    public void setHdebut(String hdebut) { this.hdebut = hdebut; }

    public String getHfin() { return hfin; }
    public void setHfin(String hfin) { this.hfin = hfin; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getMedecin() { return medecin; }
    public void setMedecin(String medecin) { this.medecin = medecin; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getPatient_id() { return patient_id; }
    public void setPatient_id(int patient_id) { this.patient_id = patient_id; }

    public int getMedecin_user_id() { return medecin_user_id; }
    public void setMedecin_user_id(int medecin_user_id) { this.medecin_user_id = medecin_user_id; }

    public Integer getFeedbackNote() { return feedbackNote; }
    public void setFeedbackNote(Integer feedbackNote) { this.feedbackNote = feedbackNote; }

    public String getFeedbackTags() { return feedbackTags; }
    public void setFeedbackTags(String feedbackTags) { this.feedbackTags = feedbackTags; }

    public String getFeedbackCommentaire() { return feedbackCommentaire; }
    public void setFeedbackCommentaire(String feedbackCommentaire) { this.feedbackCommentaire = feedbackCommentaire; }

    public String getFeedbackDate() { return feedbackDate; }
    public void setFeedbackDate(String feedbackDate) { this.feedbackDate = feedbackDate; }
    @Override
    public String toString() {
        return "rdv{" +
                "id=" + id +
                ", date='" + date + '\'' +
                ", hdebut='" + hdebut + '\'' +
                ", hfin='" + hfin + '\'' +
                ", statut='" + statut + '\'' +
                ", motif='" + motif + '\'' +
                ", medecin='" + medecin + '\'' +
                ", message='" + message + '\'' +
                ", patient_id=" + patient_id +
                ", medecin_user_id=" + medecin_user_id +
                '}';
    }
}