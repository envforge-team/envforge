package com.envforge.controlapi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permissive security configuration for local development and tests.
 *
 * Active whenever the "entra" profile is NOT set (the default). See
 * EntraSecurityConfig for the real, JWT-backed configuration used when
 * running with --spring.profiles.active=entra (Ziua 31).
 *
 * TODO (Ziua 32): once role-based access is enforced in EntraSecurityConfig,
 * consider whether this permissive dev config should also gain basic role
 * checks, or stay fully open for local development convenience.
 */
@Configuration
@EnableWebSecurity
@Profile("!entra")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
