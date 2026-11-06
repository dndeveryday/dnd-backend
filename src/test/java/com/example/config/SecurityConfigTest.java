package com.example.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {

    @Test
    void mapsKeycloakRealmRolesToSpringRoleAuthorities() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .claim("sub", "admin-user")
                .claim("realm_access", Map.of("roles", List.of("OPERATIONS")))
                .build();

        AbstractAuthenticationToken authentication = new SecurityConfig().jwtAuthenticationConverter().convert(jwt);

        assertThat(authentication.getAuthorities()).contains(new SimpleGrantedAuthority("ROLE_OPERATIONS"));
        assertThat(authentication.getAuthorities()).doesNotContain(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
    }
}