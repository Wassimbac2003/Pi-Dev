package Services;

import Models.medecin;
import Utils.MyDb;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedecinService implements ICrud<medecin> {

    private Connection connection;

    public MedecinService() {
        connection = MyDb.getInstance().getConnection();
    }

    @Override
    public void insert(medecin obj) throws SQLException {
        String sql = "INSERT INTO medecin (nom, prenom, specialite, type, disponible, photo, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, obj.getNom());
        ps.setString(2, obj.getPrenom());
        ps.setString(3, obj.getSpecialite());
        ps.setString(4, obj.getType());
        ps.setInt(5, obj.getDisponible());
        ps.setString(6, obj.getPhoto());
        ps.setInt(7, obj.getUser_id());
        ps.executeUpdate();
    }

    @Override
    public void update(medecin obj) throws SQLException {
        String sql = "UPDATE medecin SET nom=?, prenom=?, specialite=?, type=?, disponible=?, photo=?, user_id=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, obj.getNom());
        ps.setString(2, obj.getPrenom());
        ps.setString(3, obj.getSpecialite());
        ps.setString(4, obj.getType());
        ps.setInt(5, obj.getDisponible());
        ps.setString(6, obj.getPhoto());
        ps.setInt(7, obj.getUser_id());
        ps.setInt(8, obj.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(medecin obj) throws SQLException {
        String sql = "DELETE FROM medecin WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, obj.getId());
        ps.executeUpdate();
    }

    @Override
    public List<medecin> findAll() throws SQLException {
        List<medecin> list = new ArrayList<>();
        String sql = "SELECT * FROM medecin";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    // ========== MÉTHODES SPÉCIFIQUES AU WIZARD ==========

    /**
     * Récupérer toutes les spécialités distinctes
     */
    public List<String> getSpecialites() throws SQLException {
        List<String> specialites = new ArrayList<>();
        String sql = "SELECT DISTINCT specialite FROM medecin ORDER BY specialite";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            specialites.add(rs.getString("specialite"));
        }
        return specialites;
    }

    /**
     * Récupérer les médecins par spécialité
     */
    public List<medecin> findBySpecialite(String specialite) throws SQLException {
        List<medecin> list = new ArrayList<>();
        String sql = "SELECT * FROM medecin WHERE specialite = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, specialite);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Trouver un médecin par ID
     */
    public medecin findById(int id) throws SQLException {
        String sql = "SELECT * FROM medecin WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapRow(rs);
        }
        return null;
    }

    /**
     * Mapper un ResultSet vers un objet medecin
     */
    private medecin mapRow(ResultSet rs) throws SQLException {
        return new medecin(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("specialite"),
                rs.getString("type"),
                rs.getInt("disponible"),
                rs.getString("photo"),
                rs.getInt("user_id")
        );
    }
}