package com.devalere.quickbite.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration securite pour un service Spring MVC (PAS WebFlux).
 *
 * ATTENTION : le Gateway utilise EnableWebFluxSecurity.
 * Les services utilisent EnableWebSecurity (MVC classique).
 * Ce sont deux APIs differentes.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Active @PreAuthorize sur les methodes
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;

    public SecurityConfig(JwtAuthConverter jwtAuthConverter) {
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
                        // Endpoints publics
                        .requestMatchers("/actuator/**").permitAll()
                        // Endpoints par role
                        .requestMatchers("/api/orders/admin/**").hasRole("ADMIN")
                        // Tout le reste necessite une authentification
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                )
                .build();
    }
}