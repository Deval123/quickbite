package com.devalere.quickbite.gatewayservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuration securite du Gateway (WebFlux / reactif).
 *
 * ATTETION : le Gateway utilise WebFlux, pas MVC. Donc on
 * utilise ServerHttpSecurity, pas HHtSecurity.
 * Et EnableWebFluxSecurity, pas EnableWebSecurity.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig
{
    public SecurityWebFilterChain securityWebFilterChain(org.springframework.security.config.web.server.ServerHttpSecurity http) throws Exception
    {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Enpoints publics
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/api/users/register").permitAll()
                        .pathMatchers("/api/users/login").permitAll()
                        // Tout le reste necessite un JWT valide.
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }));
        return http.build();
    }
}