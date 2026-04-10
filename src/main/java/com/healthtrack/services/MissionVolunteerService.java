package com.healthtrack.services;

import com.healthtrack.entities.MissionVolunteer;
import com.healthtrack.tools.MyConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MissionVolunteerService implements IService<MissionVolunteer> {

    private final Connection connection = MyConnection.getInstance().getConnection();

    @Override
    public void ajouter(MissionVolunteer missionVolunteer) {
        String req = "INSERT INTO mission_volunteer (titre, description, lieu, date_debut, date_fin, statut, photo) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, missionVolunteer.getTitre());
            statement.setString(2, missionVolunteer.getDescription());
            statement.setString(3, missionVolunteer.getLieu());
            statement.setDate(4, missionVolunteer.getDateDebut() == null ? null : Date.valueOf(missionVolunteer.getDateDebut()));
            statement.setDate(5, missionVolunteer.getDateFin() == null ? null : Date.valueOf(missionVolunteer.getDateFin()));
            statement.setString(6, missionVolunteer.getStatut());
            statement.setString(7, missionVolunteer.getPhoto());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void supprimer(int id) {
        String req = "DELETE FROM mission_volunteer WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void modifier(MissionVolunteer missionVolunteer) {
        String req = "UPDATE mission_volunteer SET titre=?, description=?, lieu=?, date_debut=?, date_fin=?, statut=?, photo=? WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, missionVolunteer.getTitre());
            statement.setString(2, missionVolunteer.getDescription());
            statement.setString(3, missionVolunteer.getLieu());
            statement.setDate(4, missionVolunteer.getDateDebut() == null ? null : Date.valueOf(missionVolunteer.getDateDebut()));
            statement.setDate(5, missionVolunteer.getDateFin() == null ? null : Date.valueOf(missionVolunteer.getDateFin()));
            statement.setString(6, missionVolunteer.getStatut());
            statement.setString(7, missionVolunteer.getPhoto());
            statement.setInt(8, missionVolunteer.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<MissionVolunteer> getAll() {
        List<MissionVolunteer> list = new ArrayList<>();
        String req = "SELECT id, titre, description, lieu, date_debut, date_fin, statut, photo FROM mission_volunteer ORDER BY id DESC";
        try (PreparedStatement statement = connection.prepareStatement(req);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                MissionVolunteer missionVolunteer = new MissionVolunteer();
                missionVolunteer.setId(rs.getInt("id"));
                missionVolunteer.setTitre(rs.getString("titre"));
                missionVolunteer.setDescription(rs.getString("description"));
                missionVolunteer.setLieu(rs.getString("lieu"));
                Date dateDebut = rs.getDate("date_debut");
                Date dateFin = rs.getDate("date_fin");
                missionVolunteer.setDateDebut(dateDebut == null ? null : dateDebut.toLocalDate());
                missionVolunteer.setDateFin(dateFin == null ? null : dateFin.toLocalDate());
                missionVolunteer.setStatut(rs.getString("statut"));
                missionVolunteer.setPhoto(rs.getString("photo"));
                list.add(missionVolunteer);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public MissionVolunteer getOneById(int id) {
        String req = "SELECT id, titre, description, lieu, date_debut, date_fin, statut, photo FROM mission_volunteer WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    MissionVolunteer missionVolunteer = new MissionVolunteer();
                    missionVolunteer.setId(rs.getInt("id"));
                    missionVolunteer.setTitre(rs.getString("titre"));
                    missionVolunteer.setDescription(rs.getString("description"));
                    missionVolunteer.setLieu(rs.getString("lieu"));
                    Date dateDebut = rs.getDate("date_debut");
                    Date dateFin = rs.getDate("date_fin");
                    missionVolunteer.setDateDebut(dateDebut == null ? null : dateDebut.toLocalDate());
                    missionVolunteer.setDateFin(dateFin == null ? null : dateFin.toLocalDate());
                    missionVolunteer.setStatut(rs.getString("statut"));
                    missionVolunteer.setPhoto(rs.getString("photo"));
                    return missionVolunteer;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
