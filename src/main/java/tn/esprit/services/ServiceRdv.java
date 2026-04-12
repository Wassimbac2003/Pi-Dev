package tn.esprit.services;

import tn.esprit.entities.Rdv;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceRdv {

    private final Connection connection = MyConnection.getInstance().getConnection();

    private Rdv mapRow(ResultSet rs) throws SQLException {
        Rdv r = new Rdv();
        r.setId(rs.getInt("id"));
        Date d = rs.getDate("date");
        r.setDate(d != null ? d.toLocalDate() : null);
        r.setMotif(rs.getString("motif"));
        r.setStatut(rs.getString("statut"));
        int pid = rs.getInt("patient_id");
        r.setPatientId(rs.wasNull() ? null : pid);
        return r;
    }

    public Optional<Rdv> findById(int id) {
        String req = "SELECT * FROM `rdv` WHERE `id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return Optional.empty();
    }

    public List<Rdv> listByPatientId(int patientId) {
        List<Rdv> list = new ArrayList<>();
        String req = "SELECT * FROM `rdv` WHERE `patient_id` = ? ORDER BY `id` DESC";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    /** RDV minimal pour lier une ordonnance si aucun RDV n’existe encore. */
    public int insertStubForPatient(int patientId) {
        String req = "INSERT INTO `rdv` (`date`, `statut`, `motif`, `patient_id`) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            ps.setString(2, "Planifié");
            ps.setString(3, "Consultation (auto)");
            ps.setInt(4, patientId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return -1;
    }
}
