package tn.esprit.services;

import tn.esprit.entities.User;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceUser {

    private final Connection connection = MyConnection.getInstance().getConnection();

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("email"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("role")
        );
    }

    public List<User> listAll() {
        List<User> list = new ArrayList<>();
        String req = "SELECT `id`, `email`, `nom`, `prenom`, `role` FROM `user` ORDER BY `id`";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    public List<User> listMedecins() {
        List<User> list = new ArrayList<>();
        String req = "SELECT `id`, `email`, `nom`, `prenom`, `role` FROM `user` WHERE UPPER(`role`) LIKE '%MEDECIN%' ORDER BY `id`";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    /** Utilisateurs non-admin pour sélection patient (médecin / patient démo). */
    public List<User> listSelectablePatients() {
        List<User> list = new ArrayList<>();
        String req = "SELECT `id`, `email`, `nom`, `prenom`, `role` FROM `user` WHERE UPPER(`role`) NOT LIKE '%ADMIN%' ORDER BY `id`";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    /** Patients au sens strict (rôle ROLE_PATIENT). */
    public List<User> listPatientsRolePatient() {
        List<User> list = new ArrayList<>();
        String req = "SELECT `id`, `email`, `nom`, `prenom`, `role` FROM `user` WHERE TRIM(`role`) = 'ROLE_PATIENT' ORDER BY `nom`, `prenom`";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    public Optional<User> findById(int id) {
        String req = "SELECT `id`, `email`, `nom`, `prenom`, `role` FROM `user` WHERE `id` = ?";
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
}
