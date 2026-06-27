package com.devalere.quickbite.shared.security;

/**
 * Constantes partagées pour la sécurité inter-services. Utilisées par tous les services pour les hearders kafka et la
 * configuration OAuth2.
 */
public final class SecurityConstants
{
    private SecurityConstants()
    {
    }

    //Keycloak
    public static final String KEYCLOAK_REALM = "quickbite";
    public static final String KEYCLOAK_ISSUER_URI = "http://localhost:8180/realms/" + KEYCLOAK_REALM;

    //Kafka Headers
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    //Roles
    public static final String ROLE_CLIENT = "CLIENT";
    public static final String ROLE_RESTAURANT = "RESTAURANT";
    public static final String ROLE_DRIVER = "DRIVER";
    public static final String ROLE_ADMIN = "ADMIN";

}
