package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.service.OrderService;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
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
            log.error("❌ Помилка підпису вебхуку Stripe", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("❌ Помилка парсингу вебхуку", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        log.info("📥 Отримано івент від Stripe: {}", event.getType());

        if ("checkout.session.completed".equals(event.getType())) {
            log.info("🔥 Зловили checkout.session.completed!");

            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject;

            // Якщо версії API збігаються
            if (dataObjectDeserializer.getObject().isPresent()) {
                stripeObject = dataObjectDeserializer.getObject().get();
            } else {
                // Якщо версії API НЕ збігаються (наш випадок)
                log.warn("⚠️ Версії API не збігаються. Використовуємо deserializeUnsafe()");
                stripeObject = dataObjectDeserializer.deserializeUnsafe();
            }

            if (stripeObject instanceof Session session) {
                log.info("✅ Сесія розпаршена. ID: {}", session.getId());

                try {
                    orderService.processSuccessfulPayment(session.getId());
                    log.info("✅ Замовлення успішно оновлено!");
                } catch (Exception e) {
                    log.error("❌ Помилка під час оновлення замовлення: {}", e.getMessage(), e);
                    // Навіть якщо впала помилка БД, віддаємо Stripe 200,
                    // щоб він не спамив нас цим івентом 3 дні
                }

            } else {
                log.error("❌ Об'єкт не є сесією Stripe!");
            }
        }

        return ResponseEntity.ok("Success");
    }
}