package com.healthtrack.services;

import com.healthtrack.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SponsorMissionService {

    private final Connection connection = MyConnection.getInstance().getConnection();

    public List<String> getAllLinks() {
        List<String> list = new ArrayList<>();
        String req = "SELECT s.id AS sid, s.nom_societe AS sname, m.id AS mid, m.titre AS mtitre " +
                "FROM sponsor_mission_volunteer smv " +
                "JOIN sponsor s ON smv.sponsor_id = s.id " +
                "JOIN mission_volunteer m ON smv.mission_volunteer_id = m.id " +
                "ORDER BY s.id, m.id";
        try (PreparedStatement statement = connection.prepareStatement(req);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String row = rs.getInt("sid") + " - " + rs.getString("sname") + "  ->  "
                        + rs.getInt("mid") + " - " + rs.getString("mtitre");
                list.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public void ajouterLien(int sponsorId, int missionId) {
        String req = "INSERT INTO sponsor_mission_volunteer (sponsor_id, mission_volunteer_id) VALUES (?,?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, sponsorId);
            statement.setInt(2, missionId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void supprimerLien(int sponsorId, int missionId) {
        String req = "DELETE FROM sponsor_mission_volunteer WHERE sponsor_id=? AND mission_volunteer_id=?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, sponsorId);
            statement.setInt(2, missionId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
