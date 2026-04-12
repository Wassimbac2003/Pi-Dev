package com.healthtrack.services;

import com.healthtrack.entities.rdv;
import com.healthtrack.tools.MyDb;

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
        String sql = "INSERT INTO rdv (date, hdebut, hfin, statut, motif, medecin, message, patient_id, medecin_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
        ps.executeUpdate();
    }

    @Override
    public void update(rdv obj) throws SQLException {
        String sql = "UPDATE rdv SET date=?, hdebut=?, hfin=?, statut=?, motif=?, medecin=?, message=?, patient_id=?, medecin_user_id=? WHERE id=?";
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
        ps.setInt(10, obj.getId());
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
            list.add(r);
        }
        return list;
    }
    public void updateStatut(int id, String statut) throws SQLException {
        String sql = "UPDATE rdv SET statut=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, statut);
        ps.setInt(2, id);
        ps.executeUpdate();
    }
}