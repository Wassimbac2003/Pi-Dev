package com.healthtrack.services;

import com.healthtrack.entities.User;
import com.healthtrack.tools.MyConnection;
import com.healthtrack.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private final Connection connection = MyConnection.getInstance().getConnection();

    public User create(User user) {
        String sql = "INSERT INTO user (email, password, nom, prenom, adresse, telephone, skills_profile, interests_profile, availability_profile, preferred_city, action_radius_km, latitude, longitude, recommendation_weights, role, profile_picture, diploma_document, id_card_document, is_verified, verification_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindUser(ps, user, true);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de creer l'utilisateur", e);
        }
    }

    public void update(User user) {
        String sql = "UPDATE user SET email=?, password=?, nom=?, prenom=?, adresse=?, telephone=?, skills_profile=?, interests_profile=?, availability_profile=?, preferred_city=?, action_radius_km=?, latitude=?, longitude=?, recommendation_weights=?, role=?, profile_picture=?, diploma_document=?, id_card_document=?, is_verified=?, verification_status=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindUser(ps, user, false);
            ps.setInt(21, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de modifier l'utilisateur", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM user WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de supprimer l'utilisateur", e);
        }
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user ORDER BY id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire les utilisateurs", e);
        }
    }

    public User findById(int id) {
        String sql = "SELECT * FROM user WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire l'utilisateur", e);
        }
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire l'utilisateur", e);
        }
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email) != null;
    }

    public User authenticate(String email, String password) {
        User user = findByEmail(email);
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            return null;
        }
        return user;
    }

    private void bindUser(PreparedStatement ps, User user, boolean creating) throws SQLException {
        ps.setString(1, clean(user.getEmail()));
        String storedPassword = cleanPassword(user.getPassword());
        user.setPassword(storedPassword);
        ps.setString(2, storedPassword);
        ps.setString(3, clean(user.getNom()));
        ps.setString(4, clean(user.getPrenom()));
        setNullableString(ps, 5, user.getAdresse());
        setNullableString(ps, 6, user.getTelephone());
        setNullableString(ps, 7, user.getSkillsProfile());
        setNullableString(ps, 8, user.getInterestsProfile());
        setNullableString(ps, 9, user.getAvailabilityProfile());
        setNullableString(ps, 10, user.getPreferredCity());
        setNullableInteger(ps, 11, user.getActionRadiusKm());
        setNullableDouble(ps, 12, user.getLatitude());
        setNullableDouble(ps, 13, user.getLongitude());
        setNullableString(ps, 14, user.getRecommendationWeights());
        ps.setString(15, clean(user.getRole()));
        setNullableString(ps, 16, user.getProfilePicture());
        setNullableString(ps, 17, user.getDiplomaDocument());
        setNullableString(ps, 18, user.getIdCardDocument());
        ps.setBoolean(19, user.isVerified());
        setNullableString(ps, 20, user.getVerificationStatus());

        if (creating) {
            if (isBlank(user.getPassword())) {
                throw new IllegalArgumentException("Le mot de passe est obligatoire.");
            }
            if (isBlank(user.getRole())) {
                throw new IllegalArgumentException("Le role est obligatoire.");
            }
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setAdresse(rs.getString("adresse"));
        user.setTelephone(rs.getString("telephone"));
        user.setSkillsProfile(rs.getString("skills_profile"));
        user.setInterestsProfile(rs.getString("interests_profile"));
        user.setAvailabilityProfile(rs.getString("availability_profile"));
        user.setPreferredCity(rs.getString("preferred_city"));
        int radius = rs.getInt("action_radius_km");
        user.setActionRadiusKm(rs.wasNull() ? null : radius);
        double latitude = rs.getDouble("latitude");
        user.setLatitude(rs.wasNull() ? null : latitude);
        double longitude = rs.getDouble("longitude");
        user.setLongitude(rs.wasNull() ? null : longitude);
        user.setRecommendationWeights(rs.getString("recommendation_weights"));
        user.setRole(rs.getString("role"));
        user.setProfilePicture(rs.getString("profile_picture"));
        user.setDiplomaDocument(rs.getString("diploma_document"));
        user.setIdCardDocument(rs.getString("id_card_document"));
        user.setVerified(rs.getBoolean("is_verified"));
        user.setVerificationStatus(rs.getString("verification_status"));
        return user;
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (isBlank(value)) {
            ps.setNull(index, java.sql.Types.LONGVARCHAR);
        } else {
            ps.setString(index, value.trim());
        }
    }

    private void setNullableInteger(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.DOUBLE);
        } else {
            ps.setDouble(index, value);
        }
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String cleanPassword(String password) {
        if (password == null) {
            return null;
        }
        String cleaned = password.trim();
        return cleaned.startsWith("$2a$") || cleaned.startsWith("$2b$") || cleaned.startsWith("$2y$")
                ? cleaned
                : PasswordUtil.hash(cleaned);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}