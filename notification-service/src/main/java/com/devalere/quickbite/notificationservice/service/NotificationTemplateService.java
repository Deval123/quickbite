package com.devalere.quickbite.notificationservice.service;

import com.devalere.quickbite.notificationservice.model.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Resout les templates Thymeleaf par canal + eventType.
 * Templates dans : resources/templates/{canal}/{event-type}.txt ou .html
 */
@Service
public class NotificationTemplateService {

    private static final Logger log = LoggerFactory.getLogger(NotificationTemplateService.class);

    private final TemplateEngine templateEngine;

    public NotificationTemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Resout un template pour un canal et un type d'event donne.
     * Ex: canal=PUSH, eventType=OrderConfirmedEvent -> templates/push/order-confirmed.txt
     */
    public String render(NotificationChannel channel, String eventType,
                         Map<String, Object> data) {
        String templateName = resolveTemplateName(channel, eventType);

        Context context = new Context();
        context.setVariables(data);

        try {
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            log.warn("[TEMPLATE] Template introuvable : {}. Fallback message generique.",
                    templateName);
            return buildFallbackMessage(eventType, data);
        }
    }

    private String resolveTemplateName(NotificationChannel channel, String eventType) {
        // OrderConfirmedEvent -> order-confirmed
        String name = eventType
                .replace("Event", "")
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .toLowerCase();

        return channel.name().toLowerCase() + "/" + name;
    }

    private String buildFallbackMessage(String eventType, Map<String, Object> data) {
        String orderId = String.valueOf(data.getOrDefault("orderId", ""));
        return "QuickBite : " + eventType + " pour commande " + orderId;
    }
}
