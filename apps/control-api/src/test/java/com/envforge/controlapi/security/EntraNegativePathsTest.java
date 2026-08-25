package com.envforge.controlapi.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.envforge.controlapi.audit.AuditService;
import com.envforge.controlapi.user.Role;
import com.envforge.controlapi.user.UserRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Ziua 34: negative-path coverage for the Entra ID integration.
 *
 * These tests don't hit Azure — they either exercise the same
 * NimbusJwtDecoder machinery Spring Boot wires up from issuer-uri
 * (via a locally-signed token and key pair), or drive
 * EntraIdCurrentUserProvider / AuthorizationService directly, the same
 * way EntraIdCurrentUserProviderTest and AuthorizationServiceTest do.
 */
class EntraNegativePathsTest {

    private static final String BOOTSTRAP_ADMIN_EMAIL = "admin@envforge.local";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final EntraIdCurrentUserProvider provider =
        new EntraIdCurrentUserProvider(userRepository, BOOTSTRAP_ADMIN_EMAIL);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void expiredTokenIsRejectedByJwtDecoder() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        String expiredToken = signedJwt(
            privateKey, "sub-expired", Instant.now().minusSeconds(7200)
        );

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        decoder.setJwtValidator(JwtValidators.createDefault());

        JwtDecoder typedDecoder = decoder;
        assertThrows(
            JwtValidationException.class,
            () -> typedDecoder.decode(expiredToken)
        );
    }

    @Test
    void jwtMissingIdentityClaimsFallsBackToSubjectAsEmail() {
        Jwt jwt = Jwt.withTokenValue("token-value")
            .header("alg", "none")
            .subject("sub-no-claims")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
        setAuthentication(jwt);

        when(userRepository.findByExternalId("sub-no-claims"))
            .thenReturn(Optional.empty());

        CurrentUser result = provider.getCurrentUser();

        assertEquals("sub-no-claims", result.email());
        assertEquals(Role.USER, result.role());
    }

    @Test
    void jwtAuthenticatedUserWithInsufficientRoleIsDenied() {
        Jwt jwt = Jwt.withTokenValue("token-value")
            .header("alg", "none")
            .subject("sub-user")
            .claim("preferred_username", "someone@envforge.dev")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
        setAuthentication(jwt);
        when(userRepository.findByExternalId("sub-user"))
            .thenReturn(Optional.empty());

        CurrentUser currentUser = provider.getCurrentUser();
        assertEquals(Role.USER, currentUser.role());

        AuditService auditService = mock(AuditService.class);
        SecurityMetrics securityMetrics = mock(SecurityMetrics.class);
        AuthorizationService authorizationService =
            new AuthorizationService(auditService, securityMetrics);

        assertThrows(
            AccessDeniedException.class,
            () -> authorizationService.requireAdmin(currentUser, "UPDATE_USER_ROLE")
        );
    }

    private static String signedJwt(
        RSAPrivateKey privateKey, String subject, Instant expiresAt
    ) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject(subject)
            .issueTime(Date.from(expiresAt.minusSeconds(3600)))
            .expirationTime(Date.from(expiresAt))
            .build();
        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
            claims
        );
        signedJWT.sign(new RSASSASigner(privateKey));
        return signedJWT.serialize();
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static void setAuthentication(Jwt jwt) {
        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken(jwt, null));
    }
}
