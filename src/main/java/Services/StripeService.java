package Services;

import Utils.StripeConfig;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

/**
 * Service Stripe — même logique que CalendrierController.php
 * Crée des PaymentIntent et vérifie les paiements
 */
public class StripeService {

    public StripeService() {
        Stripe.apiKey = StripeConfig.SECRET_KEY;
    }

    /**
     * Créer un PaymentIntent et retourner le clientSecret
     */
    public String createPaymentIntent() throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(StripeConfig.AMOUNT)
                .setCurrency(StripeConfig.CURRENCY)
                .setDescription(StripeConfig.DESCRIPTION)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        return paymentIntent.getClientSecret();
    }

    /**
     * Vérifier qu'un paiement a bien été confirmé
     */
    public boolean verifyPayment(String paymentIntentId) {
        try {
            PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
            return "succeeded".equals(pi.getStatus());
        } catch (StripeException e) {
            System.err.println("Erreur verification Stripe : " + e.getMessage());
            return false;
        }
    }
}