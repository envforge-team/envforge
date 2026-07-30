package com.envforge.controlapi.user;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.envforge.controlapi.security.CurrentUser;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the persisted user matching the current authenticated
     * principal, creating it on first access (just-in-time provisioning).
     * Once Entra ID is wired in (Săptămâna 7), currentUser.id() will hold
     * the real Entra ID subject/oid claim instead of the debug email.
     */
    public UserEntity getOrCreateCurrentUser(CurrentUser currentUser) {
        return userRepository.findByExternalId(currentUser.id())
            .orElseGet(() -> provisionUser(currentUser));
    }

    public UserEntity updateRole(UUID userId, Role newRole) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        user.changeRole(newRole, Instant.now());
        return userRepository.save(user);
    }

    private UserEntity provisionUser(CurrentUser currentUser) {
        Instant now = Instant.now();
        UserEntity user = new UserEntity(
            UUID.randomUUID(),
            currentUser.id(),
            currentUser.email(),
            currentUser.displayName(),
            currentUser.role(),
            now,
            now
        );
        return userRepository.save(user);
    }
}
