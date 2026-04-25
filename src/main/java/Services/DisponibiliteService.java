package Services;

import Models.disponibilite;
import Utils.MyDb;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DisponibiliteService implements ICrud<disponibilite> {

    private Connection connection;

    public DisponibiliteService() {
        connection = MyDb.getInstance().getConnection();
    }

    @Override
    public void insert(disponibilite obj) throws SQLException {
        String sql = "INSERT INTO disponibilite (date_dispo, hdebut, h_fin, statut, nbr_h, med_id) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, obj.getDate_dispo());
        ps.setString(2, obj.getHdebut());
        ps.setString(3, obj.getH_fin());
        ps.setString(4, obj.getStatut());
        ps.setInt(5, obj.getNbr_h());
        ps.setInt(6, obj.getMed_id());
        ps.executeUpdate();
    }

    @Override
    public void update(disponibilite obj) throws SQLException {
        String sql = "UPDATE disponibilite SET date_dispo=?, hdebut=?, h_fin=?, statut=?, nbr_h=?, med_id=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, obj.getDate_dispo());
        ps.setString(2, obj.getHdebut());
        ps.setString(3, obj.getH_fin());
        ps.setString(4, obj.getStatut());
        ps.setInt(5, obj.getNbr_h());
        ps.setInt(6, obj.getMed_id());
        ps.setInt(7, obj.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(disponibilite obj) throws SQLException {
        String sql = "DELETE FROM disponibilite WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, obj.getId());
        ps.executeUpdate();
    }

    @Override
    public List<disponibilite> findAll() throws SQLException {
        List<disponibilite> list = new ArrayList<>();
        String sql = "SELECT * FROM disponibilite";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    // ========== MÉTHODES SPÉCIFIQUES AU WIZARD ==========

    /**
     * Récupérer les disponibilités d'un médecin pour une date précise
     * Utilisé pour : créneaux dynamiques (midi, dimanche, annulations)
     */
    public List<disponibilite> findByMedecinAndDate(int medecinId, String date) throws SQLException {
        List<disponibilite> list = new ArrayList<>();
        String sql = "SELECT * FROM disponibilite WHERE med_id = ? AND date_dispo = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, medecinId);
        ps.setString(2, date);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Mapper un ResultSet vers un objet disponibilite
     */
    private disponibilite mapRow(ResultSet rs) throws SQLException {
        return new disponibilite(
                rs.getInt("id"),
                rs.getString("date_dispo"),
                rs.getString("hdebut"),
                rs.getString("h_fin"),
                rs.getString("statut"),
                rs.getInt("nbr_h"),
                rs.getInt("med_id")
        );
    }
}