package tn.esprit.services;

import tn.esprit.entities.Fiche;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceFiche implements IService<Fiche> {

    private final Connection connection = MyConnection.getInstance().getConnection();

    private Fiche mapRow(ResultSet rs) throws SQLException {
        Fiche f = new Fiche();
        f.setId(rs.getInt("id"));
        f.setPoids(rs.getDouble("poids"));
        f.setTaille(rs.getDouble("taille"));
        f.setGrpSanguin(rs.getString("grp_sanguin"));
        f.setAllergie(rs.getString("allergie"));
        f.setMaladieChronique(rs.getString("maladie_chronique"));
        f.setTension(rs.getString("tension"));
        f.setGlycemie(rs.getDouble("glycemie"));
        Date d = rs.getDate("date");
        f.setDate(d != null ? d.toLocalDate() : null);
        f.setLibelleMaladie(rs.getString("libelle_maladie"));
        f.setGravite(rs.getString("gravite"));
        f.setRecommandation(rs.getString("recommandation"));
        f.setSymptomes(rs.getString("symptomes"));
        f.setIdUId(rs.getInt("id_u_id"));
        int mid = rs.getInt("medecin_user_id");
        f.setMedecinUserId(rs.wasNull() ? null : mid);
        return f;
    }

    @Override
    public void ajouter(Fiche f) {
        String req = "INSERT INTO `fiche` (`poids`, `taille`, `grp_sanguin`, `allergie`, `maladie_chronique`, `tension`, `glycemie`, `date`, `libelle_maladie`, `gravite`, `recommandation`, `symptomes`, `id_u_id`, `medecin_user_id`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setDouble(1, f.getPoids());
            ps.setDouble(2, f.getTaille());
            ps.setString(3, f.getGrpSanguin());
            ps.setString(4, f.getAllergie());
            ps.setString(5, f.getMaladieChronique());
            ps.setString(6, f.getTension());
            ps.setDouble(7, f.getGlycemie());
            ps.setDate(8, Date.valueOf(f.getDate()));
            ps.setString(9, f.getLibelleMaladie());
            ps.setString(10, f.getGravite());
            ps.setString(11, f.getRecommandation());
            ps.setString(12, f.getSymptomes());
            ps.setInt(13, f.getIdUId());
            if (f.getMedecinUserId() == null) {
                ps.setNull(14, Types.INTEGER);
            } else {
                ps.setInt(14, f.getMedecinUserId());
            }
            ps.executeUpdate();
            System.out.println("Fiche ajoutée.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void supprimer(Fiche f) {
        String req = "DELETE FROM `fiche` WHERE `id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, f.getId());
            int n = ps.executeUpdate();
            System.out.println(n > 0 ? "Fiche supprimée." : "Aucune fiche avec cet id.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void modifier(Fiche f) {
        String req = "UPDATE `fiche` SET `poids`=?, `taille`=?, `grp_sanguin`=?, `allergie`=?, `maladie_chronique`=?, `tension`=?, `glycemie`=?, `date`=?, `libelle_maladie`=?, `gravite`=?, `recommandation`=?, `symptomes`=?, `id_u_id`=?, `medecin_user_id`=? WHERE `id`=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setDouble(1, f.getPoids());
            ps.setDouble(2, f.getTaille());
            ps.setString(3, f.getGrpSanguin());
            ps.setString(4, f.getAllergie());
            ps.setString(5, f.getMaladieChronique());
            ps.setString(6, f.getTension());
            ps.setDouble(7, f.getGlycemie());
            ps.setDate(8, Date.valueOf(f.getDate()));
            ps.setString(9, f.getLibelleMaladie());
            ps.setString(10, f.getGravite());
            ps.setString(11, f.getRecommandation());
            ps.setString(12, f.getSymptomes());
            ps.setInt(13, f.getIdUId());
            if (f.getMedecinUserId() == null) {
                ps.setNull(14, Types.INTEGER);
            } else {
                ps.setInt(14, f.getMedecinUserId());
            }
            ps.setInt(15, f.getId());
            int n = ps.executeUpdate();
            System.out.println(n > 0 ? "Fiche modifiée." : "Aucune fiche avec cet id.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void getAll() {
        String req = "SELECT * FROM `fiche`";
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
        String req = "SELECT * FROM `fiche` WHERE `id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println(mapRow(rs));
                } else {
                    System.out.println("Aucune fiche avec id=" + id);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public List<Fiche> listAll() {
        List<Fiche> list = new ArrayList<>();
        String req = "SELECT * FROM `fiche` ORDER BY `id` DESC";
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

    public Optional<Fiche> findById(int id) {
        String req = "SELECT * FROM `fiche` WHERE `id` = ?";
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

    /** Une fiche par utilisateur (contrainte UNIQUE id_u_id). */
    public Optional<Fiche> findByUserId(int userId) {
        String req = "SELECT * FROM `fiche` WHERE `id_u_id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, userId);
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

    /** Fiches dont ce médecin est l auteur (colonne medecin_user_id). */
    public List<Fiche> listByMedecinUserId(int medecinUserId) {
        List<Fiche> list = new ArrayList<>();
        String req = "SELECT * FROM `fiche` WHERE `medecin_user_id` = ? ORDER BY `date` DESC, `id` DESC";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, medecinUserId);
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
}
