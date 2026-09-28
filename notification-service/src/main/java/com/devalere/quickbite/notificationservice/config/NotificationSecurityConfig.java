package com.devalere.quickbite.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration securite du Notification Service.
 * <p>
 * Le SharedSecurityConfig n'est pas scanne ici : sans cette config,
 * Spring Boot applique sa securite par defaut (Basic auth) et le
 * handshake WebSocket / SSE est rejete en 401.
 * <p>
 * - /ws/tracking/** : handshake WebSocket STOMP (SockJS) en acces libre
 * - /api/orders/{orderId}/stream : flux SSE en acces libre
 * - /actuator/** : health checks
 */
@Configuration
@EnableWebSecurity
public class NotificationSecurityConfig {

    @Bean
    public SecurityFilterChain notificationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/ws/tracking/**").permitAll()
                        .requestMatchers("/api/orders/*/stream").permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}
