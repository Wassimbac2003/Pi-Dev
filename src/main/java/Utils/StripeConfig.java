package Utils;

/**
 * Configuration Stripe — clés API
 * En production, ces clés devraient être dans un fichier externe ou des variables d'environnement.
 */
public class StripeConfig {

    public static final String PUBLIC_KEY = "pk_test_51T4hgyFsSom26r2IsGakSPDk8Nv0g1xOuZ5RYjkNMGqQJoRkCWySYTFZgqi7p8dvJubTFJJV88VOHlu7v3c42UTW00LbZjBrf3";
    public static final String SECRET_KEY = "sk_test_51T4hgyFsSom26r2IWPTDIDoeJmqUkCO2n9XNAs5POdTOWSBoQe1EuA8NEIgbxUk1UhLIwyVDhgg94C43t2qOMGqb00A0BetfDX";

    // Montant en centimes (70 DT = 7000 centimes, Stripe utilise EUR car TND non supporté)
    public static final long AMOUNT = 7000;
    public static final String CURRENCY = "eur";
    public static final String DESCRIPTION = "Consultation medicale - VitalTech";
}