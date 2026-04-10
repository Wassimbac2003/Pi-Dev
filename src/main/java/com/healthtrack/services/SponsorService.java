package com.healthtrack.services;

import com.healthtrack.entities.Sponsor;
import com.healthtrack.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SponsorService implements IService<Sponsor> {

    private final Connection connection = MyConnection.getInstance().getConnection();

    @Override
    public void ajouter(Sponsor sponsor) {
        String req = "INSERT INTO sponsor (nom_societe, contact_email, logo) VALUES (?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, sponsor.getNomSociete());
            statement.setString(2, sponsor.getContactEmail());
            statement.setString(3, sponsor.getLogo());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void supprimer(int id) {
        String req = "DELETE FROM sponsor WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void modifier(Sponsor sponsor) {
        String req = "UPDATE sponsor SET nom_societe=?, contact_email=?, logo=? WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, sponsor.getNomSociete());
            statement.setString(2, sponsor.getContactEmail());
            statement.setString(3, sponsor.getLogo());
            statement.setInt(4, sponsor.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Sponsor> getAll() {
        List<Sponsor> list = new ArrayList<>();
        String req = "SELECT id, nom_societe, contact_email, logo FROM sponsor ORDER BY id DESC";
        try (PreparedStatement statement = connection.prepareStatement(req);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Sponsor sponsor = new Sponsor();
                sponsor.setId(rs.getInt("id"));
                sponsor.setNomSociete(rs.getString("nom_societe"));
                sponsor.setContactEmail(rs.getString("contact_email"));
                sponsor.setLogo(rs.getString("logo"));
                list.add(sponsor);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public Sponsor getOneById(int id) {
        String req = "SELECT id, nom_societe, contact_email, logo FROM sponsor WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Sponsor sponsor = new Sponsor();
                    sponsor.setId(rs.getInt("id"));
                    sponsor.setNomSociete(rs.getString("nom_societe"));
                    sponsor.setContactEmail(rs.getString("contact_email"));
                    sponsor.setLogo(rs.getString("logo"));
                    return sponsor;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
