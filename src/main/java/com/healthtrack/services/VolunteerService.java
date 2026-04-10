package com.healthtrack.services;

import com.healthtrack.entities.Volunteer;
import com.healthtrack.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VolunteerService implements IService<Volunteer> {

    private final Connection connection = MyConnection.getInstance().getConnection();

    @Override
    public void ajouter(Volunteer volunteer) {
        String req = "INSERT INTO volunteer (motivation, disponibilites, telephone, statut, user_id, mission_id) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, volunteer.getMotivation());
            statement.setString(2, volunteer.getDisponibilitesJson());
            statement.setString(3, volunteer.getTelephone());
            statement.setString(4, volunteer.getStatut());
            statement.setInt(5, volunteer.getUserId());
            statement.setInt(6, volunteer.getMissionId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void supprimer(int id) {
        String req = "DELETE FROM volunteer WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void modifier(Volunteer volunteer) {
        String req = "UPDATE volunteer SET motivation=?, disponibilites=?, telephone=?, statut=?, user_id=?, mission_id=? WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, volunteer.getMotivation());
            statement.setString(2, volunteer.getDisponibilitesJson());
            statement.setString(3, volunteer.getTelephone());
            statement.setString(4, volunteer.getStatut());
            statement.setInt(5, volunteer.getUserId());
            statement.setInt(6, volunteer.getMissionId());
            statement.setInt(7, volunteer.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Volunteer> getAll() {
        List<Volunteer> list = new ArrayList<>();
        String req = "SELECT id, motivation, disponibilites, telephone, statut, user_id, mission_id FROM volunteer ORDER BY id DESC";
        try (PreparedStatement statement = connection.prepareStatement(req);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Volunteer volunteer = new Volunteer();
                volunteer.setId(rs.getInt("id"));
                volunteer.setMotivation(rs.getString("motivation"));
                volunteer.setDisponibilitesJson(rs.getString("disponibilites"));
                volunteer.setTelephone(rs.getString("telephone"));
                volunteer.setStatut(rs.getString("statut"));
                volunteer.setUserId(rs.getInt("user_id"));
                volunteer.setMissionId(rs.getInt("mission_id"));
                list.add(volunteer);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public Volunteer getOneById(int id) {
        String req = "SELECT id, motivation, disponibilites, telephone, statut, user_id, mission_id FROM volunteer WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Volunteer volunteer = new Volunteer();
                    volunteer.setId(rs.getInt("id"));
                    volunteer.setMotivation(rs.getString("motivation"));
                    volunteer.setDisponibilitesJson(rs.getString("disponibilites"));
                    volunteer.setTelephone(rs.getString("telephone"));
                    volunteer.setStatut(rs.getString("statut"));
                    volunteer.setUserId(rs.getInt("user_id"));
                    volunteer.setMissionId(rs.getInt("mission_id"));
                    return volunteer;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
