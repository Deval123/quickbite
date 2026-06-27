package com.devalere.quickbite.shared.security;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.kafka.common.header.Headers; // bien tenir compte du lieu de l'import de la classe
/**
 * Utilitaire pour propager le contexte utilisateur dans les headers Kafka. On ne propage PAS le JWT dans Kafka. On
 * extrait l'userId et les roles du JWT et on les met dans les headers.
 */
public final class KafkaSecurityHeaders
{
    private KafkaSecurityHeaders()
    {
    }

    /**
     * Ajoute-les headers de securité a un message Kafka.
     * À appeler dans le producer avant de publier.
     *
     * @param headers headers
     * @param userId userId
     * @param roles roles
     */
    public static void addSecurityHeaders(Headers headers, String userId, String roles)
    {
        headers.add(
                SecurityConstants.HEADER_USER_ID,
                userId.getBytes(StandardCharsets.UTF_8));
        headers.add(
                SecurityConstants.HEADER_USER_ROLES,
                roles.getBytes(StandardCharsets.UTF_8));
        headers.add(
                SecurityConstants.HEADER_CORRELATION_ID,
                UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Lit un header Kafka en String.
     * À appeler dans le consumer pour reconstruire le contexte utilisateur.
     *
     * @param headers
     * @param key
     * @return
     */
    public static String getHeader(Headers headers, String key)
    {
        var header = headers.lastHeader(key);
        if (header == null)
        {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    /**
     * Lit le userId depuis les headers Kafka.
     *
     * @param headers
     * @return
     */
    public static String getUserId(Headers headers)
    {
        return getHeader(headers, SecurityConstants.HEADER_USER_ID);
    }

    /**
     * Lit les roles depuis les headers Kafka.
     * @param headers
     * @return
     */
    public static String getRoles(Headers headers)
    {
        return getHeader(headers, SecurityConstants.HEADER_USER_ROLES);
    }
}
