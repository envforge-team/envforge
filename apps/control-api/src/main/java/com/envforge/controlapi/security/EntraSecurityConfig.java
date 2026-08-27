package com.envforge.controlapi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Real security configuration backed by Microsoft Entra ID (Ziua 31).
 *
 * Active only under the "entra" Spring profile (see
 * application-entra.properties for the issuer-uri). This keeps the
 * default/dev/test behaviour (SecurityConfig, permissive, profile
 * "!entra") completely untouched - existing @WebMvcTest slices and CI
 * keep passing without needing network access to Microsoft's identity
 * platform.
 *
 * Role-based authorization (Ziua 32) is not enforced yet here - any
 * request with a valid token issued by our Entra ID tenant is currently
 * allowed through, once authenticated.
 */
@Configuration
@EnableWebSecurity
@Profile("entra")
public class EntraSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
