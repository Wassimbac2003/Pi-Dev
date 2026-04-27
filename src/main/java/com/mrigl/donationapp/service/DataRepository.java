package com.mrigl.donationapp.service;

import com.mrigl.donationapp.model.Annonce;
import com.mrigl.donationapp.model.Donation;
import com.mrigl.donationapp.model.FraudNotification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

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

    /**
     * Backend trace for anti-fraud events (user continues publishing despite high-risk warning).
     */
    public void createFraudNotification(Annonce annonce, double score, List<String> reasons) {
        final String ddl = """
                CREATE TABLE IF NOT EXISTS fraud_notification (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    annonce_id BIGINT NULL,
                    titre_annonce VARCHAR(255) NULL,
                    risk_score DOUBLE NOT NULL,
                    reasons TEXT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    resolved BOOLEAN NOT NULL DEFAULT FALSE,
                    decision VARCHAR(80) NULL,
                    reviewed_at TIMESTAMP NULL DEFAULT NULL
                )
                """;
        final String insert = """
                INSERT INTO fraud_notification (annonce_id, titre_annonce, risk_score, reasons, resolved)
                VALUES (?, ?, ?, ?, FALSE)
                """;
        try (Connection cn = getConnection();
             PreparedStatement ddlPs = cn.prepareStatement(ddl);
             PreparedStatement ps = cn.prepareStatement(insert)) {
            ddlPs.execute();
            if (annonce == null || annonce.getId() == null) {
                ps.setNull(1, java.sql.Types.BIGINT);
            } else {
                ps.setLong(1, annonce.getId());
            }
            ps.setString(2, annonce == null ? null : annonce.getTitreAnnonce());
            ps.setDouble(3, score);
            ps.setString(4, reasons == null || reasons.isEmpty() ? null : String.join(" | ", reasons));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur JDBC lors de la création d'une notification anti-fraude", e);
        }
    }

    public List<FraudNotification> findPendingFraudNotifications() {
        ensureFraudNotificationTable();
        final String sql = """
                SELECT id, annonce_id, titre_annonce, risk_score, reasons, created_at, resolved, decision, reviewed_at
                FROM fraud_notification
                WHERE resolved = FALSE
                ORDER BY created_at ASC, id ASC
                """;
        List<FraudNotification> out = new java.util.ArrayList<>();
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FraudNotification n = new FraudNotification();
                n.setId(rs.getLong("id"));
                long annonceId = rs.getLong("annonce_id");
                n.setAnnonceId(rs.wasNull() ? null : annonceId);
                n.setTitreAnnonce(rs.getString("titre_annonce"));
                n.setRiskScore(rs.getDouble("risk_score"));
                n.setReasons(rs.getString("reasons"));
                var createdTs = rs.getTimestamp("created_at");
                n.setCreatedAt(createdTs == null ? null : createdTs.toLocalDateTime());
                n.setResolved(rs.getBoolean("resolved"));
                n.setDecision(rs.getString("decision"));
                var reviewedTs = rs.getTimestamp("reviewed_at");
                n.setReviewedAt(reviewedTs == null ? null : reviewedTs.toLocalDateTime());
                out.add(n);
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur JDBC lors du chargement des notifications anti-fraude", e);
        }
    }

    public void resolveFraudNotification(Long notificationId, String decision) {
        if (notificationId == null) {
            return;
        }
        ensureFraudNotificationTable();
        final String sql = """
                UPDATE fraud_notification
                SET resolved = TRUE, decision = ?, reviewed_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, decision == null ? "no_decision" : decision);
            ps.setLong(2, notificationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur JDBC lors de la résolution d'une notification anti-fraude", e);
        }
    }

    public Optional<Annonce> findLastAnnonceByContent(Annonce sample) {
        if (sample == null) {
            return Optional.empty();
        }
        final String sql = """
                SELECT id, titre_annonce, description, date_publication, urgence, etat_annonce
                FROM annonce
                WHERE titre_annonce = ? AND description = ? AND date_publication = ?
                ORDER BY id DESC
                LIMIT 1
                """;
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, sample.getTitreAnnonce());
            ps.setString(2, sample.getDescription());
            ps.setDate(3, sample.getDatePublication() == null ? null : Date.valueOf(sample.getDatePublication()));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Annonce a = new Annonce();
                a.setId(rs.getLong("id"));
                a.setTitreAnnonce(rs.getString("titre_annonce"));
                a.setDescription(rs.getString("description"));
                Date d = rs.getDate("date_publication");
                a.setDatePublication(d == null ? null : d.toLocalDate());
                a.setUrgence(rs.getString("urgence"));
                a.setEtatAnnonce(rs.getString("etat_annonce"));
                return Optional.of(a);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur JDBC lors de la récupération de l'annonce créée", e);
        }
    }

    private void ensureFraudNotificationTable() {
        final String ddl = """
                CREATE TABLE IF NOT EXISTS fraud_notification (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    annonce_id BIGINT NULL,
                    titre_annonce VARCHAR(255) NULL,
                    risk_score DOUBLE NOT NULL,
                    reasons TEXT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    resolved BOOLEAN NOT NULL DEFAULT FALSE,
                    decision VARCHAR(80) NULL,
                    reviewed_at TIMESTAMP NULL DEFAULT NULL
                )
                """;
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(ddl)) {
            ps.execute();
            ensureFraudNotificationColumn(cn, "resolved",
                    "ALTER TABLE fraud_notification ADD COLUMN resolved BOOLEAN NOT NULL DEFAULT FALSE");
            ensureFraudNotificationColumn(cn, "decision",
                    "ALTER TABLE fraud_notification ADD COLUMN decision VARCHAR(80) NULL");
            ensureFraudNotificationColumn(cn, "reviewed_at",
                    "ALTER TABLE fraud_notification ADD COLUMN reviewed_at TIMESTAMP NULL DEFAULT NULL");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur JDBC lors de la préparation de la table anti-fraude", e);
        }
    }

    private void ensureFraudNotificationColumn(Connection cn, String columnName, String alterSql) throws SQLException {
        final String checkSql = "SHOW COLUMNS FROM fraud_notification LIKE ?";
        try (PreparedStatement check = cn.prepareStatement(checkSql)) {
            check.setString(1, columnName);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement alter = cn.prepareStatement(alterSql)) {
            alter.execute();
        }
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
