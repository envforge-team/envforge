package com.envforge.controlapi.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByExternalId(String externalId);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
