package Services;

import Models.rdv;
import Utils.MyDb;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RdvService implements ICrud<rdv> {

    private Connection connection;

    public RdvService() {
        connection = MyDb.getInstance().getConnection();
    }

    @Override
    public void insert(rdv obj) throws SQLException {
        String sql = "INSERT INTO rdv (date, hdebut, hfin, statut, motif, medecin, message, patient_id, medecin_user_id, type_consultation) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, obj.getDate());
        ps.setString(2, obj.getHdebut());
        ps.setString(3, obj.getHfin());
        ps.setString(4, obj.getStatut());
        ps.setString(5, obj.getMotif());
        ps.setString(6, obj.getMedecin());
        ps.setString(7, obj.getMessage());
        ps.setInt(8, obj.getPatient_id());
        ps.setInt(9, obj.getMedecin_user_id());
        ps.setString(10, obj.getTypeConsultation() != null ? obj.getTypeConsultation() : "presentiel");
        ps.executeUpdate();
    }

    @Override
    public void update(rdv obj) throws SQLException {
        String sql = "UPDATE rdv SET date=?, hdebut=?, hfin=?, statut=?, motif=?, medecin=?, " +
                "message=?, patient_id=?, medecin_user_id=?, type_consultation=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, obj.getDate());
        ps.setString(2, obj.getHdebut());
        ps.setString(3, obj.getHfin());
        ps.setString(4, obj.getStatut());
        ps.setString(5, obj.getMotif());
        ps.setString(6, obj.getMedecin());
        ps.setString(7, obj.getMessage());
        ps.setInt(8, obj.getPatient_id());
        ps.setInt(9, obj.getMedecin_user_id());
        ps.setString(10, obj.getTypeConsultation() != null ? obj.getTypeConsultation() : "presentiel");
        ps.setInt(11, obj.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(rdv obj) throws SQLException {
        String sql = "DELETE FROM rdv WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, obj.getId());
        ps.executeUpdate();
    }

    @Override
    public List<rdv> findAll() throws SQLException {
        List<rdv> list = new ArrayList<>();
        String sql = "SELECT * FROM rdv";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<rdv> findByMedecinUserId(int medecinUserId) throws SQLException {
        List<rdv> rdvs = new ArrayList<>();
        String sql = "SELECT * FROM rdv WHERE medecin_user_id = ? ORDER BY date DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, medecinUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rdvs.add(mapResultSet(rs));
            }
        }
        return rdvs;
    }

    public void updateStatut(int id, String statut) throws SQLException {
        String sql = "UPDATE rdv SET statut=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, statut);
        ps.setInt(2, id);
        ps.executeUpdate();
    }

    public void updateFeedback(int id, int note, String tags, String commentaire) throws SQLException {
        String sql = "UPDATE rdv SET feedback_note=?, feedback_tags=?, feedback_commentaire=?, " +
                "feedback_date=NOW(), statut='Termine' WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, note);
        ps.setString(2, tags);
        ps.setString(3, commentaire);
        ps.setInt(4, id);
        ps.executeUpdate();
    }

    // ══════════════════════════════════════════════
    //  HELPER — mapper ResultSet → objet rdv
    // ══════════════════════════════════════════════
    private rdv mapResultSet(ResultSet rs) throws SQLException {
        rdv r = new rdv(
                rs.getInt("id"),
                rs.getString("date"),
                rs.getString("hdebut"),
                rs.getString("hfin"),
                rs.getString("statut"),
                rs.getString("motif"),
                rs.getString("medecin"),
                rs.getString("message"),
                rs.getInt("patient_id"),
                rs.getInt("medecin_user_id")
        );

        // Feedback
        r.setFeedbackNote(rs.getObject("feedback_note") != null ? rs.getInt("feedback_note") : null);
        r.setFeedbackTags(rs.getString("feedback_tags"));
        r.setFeedbackCommentaire(rs.getString("feedback_commentaire"));
        r.setFeedbackDate(rs.getString("feedback_date"));

        // Type consultation
        r.setTypeConsultation(rs.getString("type_consultation"));

        return r;
    }
}