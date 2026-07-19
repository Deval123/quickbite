package com.devalere.quickbite.kafka;

/**
 * Noms des topics kafka utilisées dans QuickBite.
 * Centralisées ici pour éviter les fautes de frappe entre services.
 */
public final class KafkaTopics {
    public static final String ORDER_EVENTS = "order-events";
    public static final String PAYMENT_EVENTS = "payment-events";
    public static final String RESTAURANT_EVENTS = "restaurant-events";
    public static final String DELIVERY_EVENTS = "delivery-events";
    public static final String NOTIFICATION_EVENTS = "notification-events";

    public static final int PARTITIONS = 3;
    public static final short REPLICATION_FACTOR = 1;

}

