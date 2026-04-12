package tn.esprit.services;

import tn.esprit.entities.Medicament;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceMedicament implements IService<Medicament> {

    private final Connection connection = MyConnection.getInstance().getConnection();

    private Medicament mapRow(ResultSet rs) throws SQLException {
        Medicament m = new Medicament();
        m.setId(rs.getInt("id"));
        m.setNomMedicament(rs.getString("nom_medicament"));
        m.setCategorie(rs.getString("categorie"));
        m.setDosage(rs.getString("dosage"));
        m.setForme(rs.getString("forme"));
        Date d = rs.getDate("date_expiration");
        m.setDateExpiration(d != null ? d.toLocalDate() : null);
        return m;
    }

    @Override
    public void ajouter(Medicament m) {
        String req = "INSERT INTO `medicament` (`nom_medicament`, `categorie`, `dosage`, `forme`, `date_expiration`) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, m.getNomMedicament());
            ps.setString(2, m.getCategorie());
            ps.setString(3, m.getDosage());
            ps.setString(4, m.getForme());
            ps.setDate(5, Date.valueOf(m.getDateExpiration()));
            ps.executeUpdate();
            System.out.println("Médicament ajouté.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void supprimer(Medicament m) {
        String req = "DELETE FROM `medicament` WHERE `id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, m.getId());
            int n = ps.executeUpdate();
            System.out.println(n > 0 ? "Médicament supprimé." : "Aucun médicament avec cet id.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void modifier(Medicament m) {
        String req = "UPDATE `medicament` SET `nom_medicament`=?, `categorie`=?, `dosage`=?, `forme`=?, `date_expiration`=? WHERE `id`=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, m.getNomMedicament());
            ps.setString(2, m.getCategorie());
            ps.setString(3, m.getDosage());
            ps.setString(4, m.getForme());
            ps.setDate(5, Date.valueOf(m.getDateExpiration()));
            ps.setInt(6, m.getId());
            int n = ps.executeUpdate();
            System.out.println(n > 0 ? "Médicament modifié." : "Aucun médicament avec cet id.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void getAll() {
        String req = "SELECT * FROM `medicament`";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                System.out.println(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void getOneById(int id) {
        String req = "SELECT * FROM `medicament` WHERE `id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println(mapRow(rs));
                } else {
                    System.out.println("Aucun médicament avec id=" + id);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public List<Medicament> listAll() {
        List<Medicament> list = new ArrayList<>();
        String req = "SELECT * FROM `medicament` ORDER BY `id` DESC";
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

    public Optional<Medicament> findById(int id) {
        String req = "SELECT * FROM `medicament` WHERE `id` = ?";
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
