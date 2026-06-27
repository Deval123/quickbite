package com.devalere.quickbite.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration WebClient pour les appels service-to-service
 * avec authentification client_credentials automatique.
 *
 * Order Service appelle Restaurant Service pour verifier le menu.
 * Le WebClient obtient automatiquement un token client_credentials
 * aupres de Keycloak et l'ajoute dans le header Authorization.
 *
 * IMPORTANT : utilise les types Servlet (pas Reactive) car order-service
 * est une app Spring MVC, pas WebFlux.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(ClientRegistrationRepository clientRegistrations,
                               OAuth2AuthorizedClientService authorizedClientService) {

        var clientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrations,
                authorizedClientService
        );

        var oauth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(clientManager);
        oauth2Filter.setDefaultClientRegistrationId("keycloak-service");

        return WebClient.builder()
                .filter(oauth2Filter)
                .build();
    }
}