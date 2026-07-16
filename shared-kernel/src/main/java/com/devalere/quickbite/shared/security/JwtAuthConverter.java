package com.devalere.quickbite.shared.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Convertisseur JWT pour extraire les roles Keycloak.
 *
 * Par defaut, Spring Security cherche les authorities dans le claim "scope".
 * Keycloak met les roles dans "realm_access.roles".
 * Ce converter mappe les deux.
 *
 * Place dans le shared-kernel pour etre reutilise par tous les services MVC.
 * Le Gateway (WebFlux) a son propre converter reactif.
 */
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultConverter =
            new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Roles par defaut (scopes)
        Collection<GrantedAuthority> defaultAuthorities =
                defaultConverter.convert(jwt);

        // Roles Keycloak (realm_access.roles)
        Collection<GrantedAuthority> keycloakAuthorities =
                extractKeycloakRoles(jwt);

        // Combiner les deux
        Collection<GrantedAuthority> allAuthorities = Stream.concat(
                defaultAuthorities != null ? defaultAuthorities.stream() : Stream.empty(),
                keycloakAuthorities.stream()
        ).collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, allAuthorities, jwt.getClaimAsString("sub"));
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return Collections.emptyList();
        }

        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) {
            return Collections.emptyList();
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}
