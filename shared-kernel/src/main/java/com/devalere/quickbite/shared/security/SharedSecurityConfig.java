package com.devalere.quickbite.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration securite partagee pour tous les services MVC.
 *
 * Chaque service qui depend du shared-kernel herite automatiquement
 * de cette config :
 * - CSRF desactive (API stateless)
 * - Sessions stateless (pas de cookie de session)
 * - /actuator/** en acces libre (health checks)
 * - Tout le reste necessite un JWT valide
 * - @PreAuthorize / @PostAuthorize actifs sur les controllers
 * - JwtAuthConverter pour mapper les roles Keycloak (realm_access.roles)
 *
 * ATTENTION : cette config est pour les services MVC uniquement.
 * Le Gateway utilise WebFlux et a sa propre config (EnableWebFluxSecurity).
 * Ne PAS ajouter le shared-kernel au gateway-service.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Active @PreAuthorize / @PostAuthorize sur les methodes
public class SharedSecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;

    public SharedSecurityConfig(JwtAuthConverter jwtAuthConverter) {
        this.jwtAuthConverter = jwtAuthConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                )
                .build();
    }
}
