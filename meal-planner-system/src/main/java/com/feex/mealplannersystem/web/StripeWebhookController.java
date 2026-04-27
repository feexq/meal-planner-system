package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.service.OrderService;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final OrderService orderService;

    @Value("${spring.stripe.webhook.secret}")
    private String endpointSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) throws EventDataObjectDeserializationException {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Error parsing webhook payload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        log.info("Received Stripe event: {}", event.getType());

        if ("checkout.session.completed".equals(event.getType())) {
            log.info("Processing checkout.session.completed event");

            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject;

            if (dataObjectDeserializer.getObject().isPresent()) {
                stripeObject = dataObjectDeserializer.getObject().get();
            } else {
                log.warn("API version mismatch. Using deserializeUnsafe()");
                stripeObject = dataObjectDeserializer.deserializeUnsafe();
            }

            if (stripeObject instanceof Session session) {
                log.info("Session parsed successfully. ID: {}", session.getId());

                try {
                    orderService.processSuccessfulPayment(session.getId());
                    log.info("Order successfully updated for session ID: {}", session.getId());
                } catch (Exception e) {
                    log.error("Error updating order for session: {}", e.getMessage(), e);
                }

            } else {
                log.error("Stripe object is not a Session instance");
            }
        } else if ("payment_intent.succeeded".equals(event.getType())) {
            log.info("Processing payment_intent.succeeded event");

            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject;

            if (dataObjectDeserializer.getObject().isPresent()) {
                stripeObject = dataObjectDeserializer.getObject().get();
            } else {
                log.warn("API version mismatch. Using deserializeUnsafe() for PaymentIntent");
                stripeObject = dataObjectDeserializer.deserializeUnsafe();
            }

            if (stripeObject instanceof PaymentIntent intent) {
                log.info("PaymentIntent parsed successfully. ID: {}", intent.getId());
                try {
                    orderService.processSuccessfulPayment(intent.getId());
                    log.info("Order successfully updated via PaymentIntent: {}", intent.getId());
                } catch (Exception e) {
                    log.error("Error updating order for PaymentIntent: {}", e.getMessage(), e);
                }
            } else {
                log.error("Stripe object is not a PaymentIntent instance");
            }
        }

        return ResponseEntity.ok("Success");
    }
}
