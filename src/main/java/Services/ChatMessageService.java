package Services;

import Models.ChatMessage;
import Utils.MyDb;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageService {

    private Connection connection;

    public ChatMessageService() {
        this.connection = MyDb.getInstance().getConnection();
    }

    /**
     * Sauvegarde un message en base de données
     */
    public void sauvegarderMessage(String senderId, String receiverId, String message) {
        String sql = "INSERT INTO chat_messages (sender_id, receiver_id, message) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, senderId);
            ps.setString(2, receiverId);
            ps.setString(3, message);
            ps.executeUpdate();
            System.out.println("💾 [DB] Message sauvegardé : " + senderId + " → " + receiverId);
        } catch (SQLException e) {
            System.err.println("❌ [DB] Erreur sauvegarde message : " + e.getMessage());
        }
    }

    /**
     * Récupère l'historique entre deux utilisateurs
     */
    public List<ChatMessage> getHistorique(String userId1, String userId2) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT * FROM chat_messages " +
                "WHERE (sender_id = ? AND receiver_id = ?) " +
                "OR (sender_id = ? AND receiver_id = ?) " +
                "ORDER BY timestamp ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId1);
            ps.setString(2, userId2);
            ps.setString(3, userId2);
            ps.setString(4, userId1);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ChatMessage msg = new ChatMessage();
                msg.setId(rs.getInt("id"));
                msg.setSenderId(rs.getString("sender_id"));
                msg.setReceiverId(rs.getString("receiver_id"));
                msg.setMessage(rs.getString("message"));
                msg.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                msg.setRead(rs.getBoolean("is_read"));
                messages.add(msg);
            }
            System.out.println("📜 [DB] Historique chargé : " + messages.size() + " messages");
        } catch (SQLException e) {
            System.err.println("❌ [DB] Erreur chargement historique : " + e.getMessage());
        }
        return messages;
    }

    /**
     * Marque les messages comme lus (sender → receiver)
     */
    public void marquerCommeLu(String receiverId, String senderId) {
        String sql = "UPDATE chat_messages SET is_read = TRUE " +
                "WHERE receiver_id = ? AND sender_id = ? AND is_read = FALSE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, receiverId);
            ps.setString(2, senderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ [DB] Erreur marquage lu : " + e.getMessage());
        }
    }

    /**
     * Compte les messages non lus envoyés par un patient spécifique au médecin
     */
    public int getUnreadCount(String medecinId, String patientId) {
        String sql = "SELECT COUNT(*) FROM chat_messages " +
                "WHERE receiver_id = ? AND sender_id = ? AND is_read = FALSE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, medecinId);
            ps.setString(2, patientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ [DB] getUnreadCount : " + e.getMessage());
        }
        return 0;
    }

    /**
     * Compte TOUS les messages non lus pour ce médecin (toutes conversations)
     */
    public int getTotalUnread(String medecinId) {
        String sql = "SELECT COUNT(*) FROM chat_messages " +
                "WHERE receiver_id = ? AND is_read = FALSE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, medecinId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ [DB] getTotalUnread : " + e.getMessage());
        }
        return 0;
    }
}