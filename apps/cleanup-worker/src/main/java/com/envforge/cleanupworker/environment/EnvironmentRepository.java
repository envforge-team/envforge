package com.envforge.cleanupworker.environment;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnvironmentRepository
        extends JpaRepository<EnvironmentEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select environment
            from EnvironmentEntity environment
            where environment.id = :id
            """)
    Optional<EnvironmentEntity> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select environment
            from EnvironmentEntity environment
            where environment.expiresAt <= :now
              and environment.status in :statuses
            order by environment.expiresAt
            """)
    List<EnvironmentEntity> findExpiredForUpdate(
            @Param("now") Instant now,
            @Param("statuses") Collection<EnvironmentStatus> statuses,
            Pageable pageable
    );
}
