package tn.esprit.services;

import tn.esprit.entities.Ordonnance;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceOrdonnance implements IService<Ordonnance> {

    private final Connection connection = MyConnection.getInstance().getConnection();
    private final ServiceLigneOrdonnance ligneService = new ServiceLigneOrdonnance();

    private Ordonnance mapRow(ResultSet rs) throws SQLException {
        Ordonnance o = new Ordonnance();
        o.setId(rs.getInt("id"));
        o.setPosologie(rs.getString("posologie"));
        o.setFrequence(rs.getString("frequence"));
        o.setDureeTraitement(rs.getInt("duree_traitement"));
        Timestamp ts = rs.getTimestamp("date_ordonnance");
        o.setDateOrdonnance(ts != null ? ts.toLocalDateTime() : null);
        o.setScanToken(rs.getString("scan_token"));
        o.setIdUId(rs.getInt("id_u_id"));
        int rid = rs.getInt("id_rdv_id");
        o.setIdRdvId(rs.wasNull() ? null : rid);
        o.setIdFicheId(rs.getInt("id_fiche_id"));
        int mid = rs.getInt("medecin_user_id");
        o.setMedecinUserId(rs.wasNull() ? null : mid);
        return o;
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer v) throws SQLException {
        if (v == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, v);
        }
    }

    @Override
    public void ajouter(Ordonnance o) {
        ajouterRetourId(o);
    }

    /** Insère une ordonnance et retourne l id généré, ou -1 en cas d échec. */
    public int ajouterRetourId(Ordonnance o) {
        String req = "INSERT INTO `ordonnance` (`posologie`, `frequence`, `duree_traitement`, `date_ordonnance`, `scan_token`, `id_u_id`, `id_rdv_id`, `id_fiche_id`, `medecin_user_id`) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, o.getPosologie());
            ps.setString(2, o.getFrequence());
            ps.setInt(3, o.getDureeTraitement());
            ps.setTimestamp(4, Timestamp.valueOf(o.getDateOrdonnance()));
            ps.setString(5, o.getScanToken());
            ps.setInt(6, o.getIdUId());
            setNullableInt(ps, 7, o.getIdRdvId());
            ps.setInt(8, o.getIdFicheId());
            setNullableInt(ps, 9, o.getMedecinUserId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    System.out.println("Ordonnance ajoutée id=" + id);
                    return id;
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return -1;
    }

    @Override
    public void supprimer(Ordonnance o) {
        ligneService.deleteByOrdonnanceId(o.getId());
        String req = "DELETE FROM `ordonnance` WHERE `id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, o.getId());
            int n = ps.executeUpdate();
            System.out.println(n > 0 ? "Ordonnance supprimée." : "Aucune ordonnance avec cet id.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void modifier(Ordonnance o) {
        String req = "UPDATE `ordonnance` SET `posologie`=?, `frequence`=?, `duree_traitement`=?, `date_ordonnance`=?, `scan_token`=?, `id_u_id`=?, `id_rdv_id`=?, `id_fiche_id`=?, `medecin_user_id`=? WHERE `id`=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, o.getPosologie());
            ps.setString(2, o.getFrequence());
            ps.setInt(3, o.getDureeTraitement());
            ps.setTimestamp(4, Timestamp.valueOf(o.getDateOrdonnance()));
            ps.setString(5, o.getScanToken());
            ps.setInt(6, o.getIdUId());
            setNullableInt(ps, 7, o.getIdRdvId());
            ps.setInt(8, o.getIdFicheId());
            setNullableInt(ps, 9, o.getMedecinUserId());
            ps.setInt(10, o.getId());
            int n = ps.executeUpdate();
            System.out.println(n > 0 ? "Ordonnance modifiée." : "Aucune ordonnance avec cet id.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void getAll() {
        String req = "SELECT * FROM `ordonnance`";
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
        String req = "SELECT * FROM `ordonnance` WHERE `id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println(mapRow(rs));
                } else {
                    System.out.println("Aucune ordonnance avec id=" + id);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public List<Ordonnance> listAll() {
        List<Ordonnance> list = new ArrayList<>();
        String req = "SELECT * FROM `ordonnance` ORDER BY `id` DESC";
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

    public List<Ordonnance> listByUserId(int userId) {
        List<Ordonnance> list = new ArrayList<>();
        String req = "SELECT * FROM `ordonnance` WHERE `id_u_id` = ? ORDER BY `id` DESC";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, userId);
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

    public Optional<Ordonnance> findById(int id) {
        String req = "SELECT * FROM `ordonnance` WHERE `id` = ?";
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

    /** Ordonnances rédigées par ce médecin (medecin_user_id). */
    public List<Ordonnance> listByMedecinUserId(int medecinUserId) {
        List<Ordonnance> list = new ArrayList<>();
        String req = "SELECT * FROM `ordonnance` WHERE `medecin_user_id` = ? ORDER BY `date_ordonnance` DESC, `id` DESC";
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
