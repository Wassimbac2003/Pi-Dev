package Services;

import com.healthtrack.entities.rdv;
import com.healthtrack.services.RdvService;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RdvServiceTest {

    static RdvService service;
    static int idRdvTest;

    // S'exécute une seule fois avant tous les tests
    @BeforeAll
    static void setup() {
        service = new RdvService();
    }

    // Test 1 : Ajouter un RDV
    @Test
    @Order(1)
    void testAjouterRdv() throws SQLException {
        rdv r = new rdv(
                "2026-04-10",   // date
                "09:00",        // hdebut
                "09:30",        // hfin
                "en_attente",   // statut
                "Consultation", // motif
                "Dr. Test",     // medecin
                "Test message", // message
                1,              // patient_id
                1               // medecin_user_id
        );
        service.insert(r);

        // Vérifier que le RDV a été ajouté
        List<rdv> rdvs = service.findAll();
        assertFalse(rdvs.isEmpty());
        assertTrue(
                rdvs.stream().anyMatch(rv -> rv.getMedecin().equals("Dr. Test"))
        );

        // Récupérer l'ID pour les prochains tests
        idRdvTest = rdvs.stream()
                .filter(rv -> rv.getMedecin().equals("Dr. Test"))
                .findFirst()
                .get()
                .getId();

        System.out.println("RDV ajouté avec ID : " + idRdvTest);
    }

    // Test 2 : Modifier un RDV
    @Test
    @Order(2)
    void testModifierRdv() throws SQLException {
        rdv r = new rdv(
                idRdvTest,
                "2026-04-15",
                "10:00",
                "10:30",
                "confirme",
                "Suivi",
                "Dr. Modifie",
                "Message modifié",
                1,
                1
        );
        service.update(r);

        // Vérifier que la modification a été appliquée
        List<rdv> rdvs = service.findAll();
        boolean trouve = rdvs.stream()
                .anyMatch(rv -> rv.getMedecin().equals("Dr. Modifie"));
        assertTrue(trouve);

        System.out.println("RDV modifié avec succès");
    }

    // Test 3 : Afficher tous les RDV
    @Test
    @Order(3)
    void testAfficherRdvs() throws SQLException {
        List<rdv> rdvs = service.findAll();
        assertNotNull(rdvs);
        assertFalse(rdvs.isEmpty());

        System.out.println("Nombre de RDVs : " + rdvs.size());
    }

    // Test 4 : Supprimer un RDV
    @Test
    @Order(4)
    void testSupprimerRdv() throws SQLException {
        rdv r = new rdv();
        r.setId(idRdvTest);
        service.delete(r);

        // Vérifier que le RDV n'existe plus
        List<rdv> rdvs = service.findAll();
        boolean existe = rdvs.stream()
                .anyMatch(rv -> rv.getId() == idRdvTest);
        assertFalse(existe);

        System.out.println("RDV supprimé avec succès");
    }
}