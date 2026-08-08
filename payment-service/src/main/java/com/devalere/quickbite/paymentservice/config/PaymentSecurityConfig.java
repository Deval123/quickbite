package com.devalere.quickbite.paymentservice.config;

import com.devalere.quickbite.shared.security.JwtAuthConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Override du SharedSecurityConfig pour le PaymentService.
 *
 * Ajoute une exception pour l'endpoint webhook Stripe
 * qui doit etre accessible sans JWT (Stripe ne peut pas
 * envoyer de token d'authentification).
 *
 * La verification d'authenticite est faite par la signature
 * HMAC dans le header Stripe-Signature.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class PaymentSecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;

    public PaymentSecurityConfig(JwtAuthConverter jwtAuthConverter) {
        this.jwtAuthConverter = jwtAuthConverter;
    }

    @Bean
    @Primary
    public SecurityFilterChain paymentSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/webhooks/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                )
                .build();
    }
}
