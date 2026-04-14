package com.mrigl.donationapp.service;

import com.mrigl.donationapp.model.Annonce;
import com.mrigl.donationapp.model.Donation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Singleton : accès unique aux données en mémoire (équivalent repository Symfony).
 */
public final class DataRepository {

    private static volatile DataRepository instance;

    private final ObservableList<Annonce> annonces = FXCollections.observableArrayList();
    private final ObservableList<Donation> donations = FXCollections.observableArrayList();

    private static final String DB_URL = System.getenv().getOrDefault(
            "JAVA_APP_DB_URL", "jdbc:mysql://127.0.0.1:3306/annonce_db?serverTimezone=UTC");
    private static final String DB_USER = System.getenv().getOrDefault("JAVA_APP_DB_USER", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("JAVA_APP_DB_PASSWORD", "");
    private volatile String startupDbError;

    private DataRepository() {
        try {
            loadAllFromDatabase();
        } catch (RuntimeException e) {
            startupDbError = e.getMessage();
            annonces.clear();
            donations.clear();
            System.err.println("JDBC startup warning: " + e.getMessage());
        }
    }

    public static DataRepository getInstance() {
        if (instance == null) {
            synchronized (DataRepository.class) {
                if (instance == null) {
                    instance = new DataRepository();
                }
            }
        }
        return instance;
    }

    public ObservableList<Annonce> annoncesProperty() {
        return annonces;
    }

    public ObservableList<Donation> donationsProperty() {
        return donations;
    }

    public Optional<String> getStartupDbError() {
        return Optional.ofNullable(startupDbError);
    }

    public void createAnnonce(Annonce a) {
        final String sql = """
                INSERT INTO annonce (titre_annonce, description, date_publication, urgence, etat_annonce)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, a.getTitreAnnonce());
            ps.setString(2, a.getDescription());
            ps.setDate(3, Date.valueOf(a.getDatePublication()));
            ps.setString(4, a.getUrgence());
            ps.setString(5, a.getEtatAnnonce());
            ps.executeUpdate();
            loadAllFromDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur JDBC lors de la création d'une annonce", e);
        }
    }

    public void updateAnnonce(Annonce a) {
        final String sql = """
                UPDATE annonce
                SET titre_annonce = ?, description = ?, date_publication = ?, urgence = ?, etat_annonce = ?
                WHERE id = ?
                """;
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, a.getTitreAnnonce());
            ps.setString(2, a.getDescription());
            ps.setDate(3, Date.valueOf(a.getDatePublication()));
            ps.setString(4, a.getUrgence());
            ps.setString(5, a.getEtatAnnonce());
            ps.setLong(6, a.getId());
            ps.executeUpdate();
            loadAllFromDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur JDBC lors de la modification d'une annonce", e);
        }
    }

    public void deleteAnnonce(Long id) {
        final String sql = "DELETE FROM annonce WHERE id = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            loadAllFromDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur JDBC lors de la suppression d'une annonce", e);
        }
    }

    public Optional<Annonce> findAnnonce(Long id) {
        return annonces.stream().filter(a -> id.equals(a.getId())).findFirst();
    }

    public void createDonation(Donation d) {
        final String sql = """
                INSERT INTO donation (type_don, quantite, date_donation, statut, annonce_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, d.getTypeDon());
            ps.setInt(2, d.getQuantite());
            ps.setDate(3, Date.valueOf(d.getDateDonation()));
            ps.setString(4, d.getStatut());
            if (d.getAnnonceId() == null) {
                ps.setNull(5, java.sql.Types.INTEGER);
            } else {
                ps.setLong(5, d.getAnnonceId());
            }
            ps.executeUpdate();
            loadAllFromDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur JDBC lors de la création d'une donation", e);
        }
    }

    public void updateDonation(Donation d) {
        final String sql = """
                UPDATE donation
                SET type_don = ?, quantite = ?, date_donation = ?, statut = ?, annonce_id = ?
                WHERE id = ?
                """;
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, d.getTypeDon());
            ps.setInt(2, d.getQuantite());
            ps.setDate(3, Date.valueOf(d.getDateDonation()));
            ps.setString(4, d.getStatut());
            if (d.getAnnonceId() == null) {
                ps.setNull(5, java.sql.Types.INTEGER);
            } else {
                ps.setLong(5, d.getAnnonceId());
            }
            ps.setLong(6, d.getId());
            ps.executeUpdate();
            loadAllFromDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur JDBC lors de la modification d'une donation", e);
        }
    }

    public void deleteDonation(Long id) {
        final String sql = "DELETE FROM donation WHERE id = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            loadAllFromDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur JDBC lors de la suppression d'une donation", e);
        }
    }

    public Optional<Donation> findDonation(Long id) {
        return donations.stream().filter(d -> id.equals(d.getId())).findFirst();
    }

    private void loadAllFromDatabase() {
        annonces.setAll(loadAnnonces());
        donations.setAll(loadDonations());
    }

    private ObservableList<Annonce> loadAnnonces() {
        ObservableList<Annonce> list = FXCollections.observableArrayList();
        final String sql = """
                SELECT id, titre_annonce, description, date_publication, urgence, etat_annonce
                FROM annonce
                ORDER BY id DESC
                """;
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Annonce a = new Annonce();
                a.setId(rs.getLong("id"));
                a.setTitreAnnonce(rs.getString("titre_annonce"));
                a.setDescription(rs.getString("description"));
                Date d = rs.getDate("date_publication");
                a.setDatePublication(d == null ? null : d.toLocalDate());
                a.setUrgence(rs.getString("urgence"));
                a.setEtatAnnonce(rs.getString("etat_annonce"));
                list.add(a);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur JDBC lors du chargement des annonces", e);
        }
    }

    private ObservableList<Donation> loadDonations() {
        ObservableList<Donation> list = FXCollections.observableArrayList();
        final String sql = """
                SELECT id, type_don, quantite, date_donation, statut, annonce_id
                FROM donation
                ORDER BY id DESC
                """;
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Donation d = new Donation();
                d.setId(rs.getLong("id"));
                d.setTypeDon(rs.getString("type_don"));
                d.setQuantite(rs.getInt("quantite"));
                Date dateDon = rs.getDate("date_donation");
                d.setDateDonation(dateDon == null ? null : dateDon.toLocalDate());
                d.setStatut(rs.getString("statut"));
                long annonceId = rs.getLong("annonce_id");
                d.setAnnonceId(rs.wasNull() ? null : annonceId);
                list.add(d);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur JDBC lors du chargement des donations", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
