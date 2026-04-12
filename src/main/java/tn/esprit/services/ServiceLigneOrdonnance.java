package tn.esprit.services;

import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceLigneOrdonnance {

    private final Connection connection = MyConnection.getInstance().getConnection();

    public void deleteByOrdonnanceId(int ordonnanceId) {
        String req = "DELETE FROM `ligne_ordonnance` WHERE `ordonnance_id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, ordonnanceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    /** Ligne minimale pour lier un médicament à une ordonnance. */
    public void insertLigne(int ordonnanceId, int medicamentId) {
        String req = "INSERT INTO `ligne_ordonnance` (`nb_jours`, `frequence_par_jour`, `moment_prise`, `avant_repas`, `periode`, `ordonnance_id`, `medicament_id`) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, 7);
            ps.setInt(2, 1);
            ps.setString(3, "Selon prescription");
            ps.setInt(4, 0);
            ps.setString(5, "Quotidien");
            ps.setInt(6, ordonnanceId);
            ps.setInt(7, medicamentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public List<Integer> listMedicamentIdsByOrdonnance(int ordonnanceId) {
        List<Integer> ids = new ArrayList<>();
        String req = "SELECT `medicament_id` FROM `ligne_ordonnance` WHERE `ordonnance_id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, ordonnanceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("medicament_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return ids;
    }
}
