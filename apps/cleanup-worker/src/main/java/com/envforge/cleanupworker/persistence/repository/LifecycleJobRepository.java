package com.envforge.cleanupworker.persistence.repository;

import com.envforge.cleanupworker.domain.LifecycleJobStatus;
import com.envforge.cleanupworker.persistence.entity.LifecycleJobEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LifecycleJobRepository
        extends JpaRepository<LifecycleJobEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select job
            from LifecycleJobEntity job
            where job.status = :queued
               or (
                    job.status = :retrying
                    and job.nextRetryAt <= :now
               )
            order by job.createdAt
            """)
    List<LifecycleJobEntity> findReadyJobsForUpdate(
            @Param("queued") LifecycleJobStatus queued,
            @Param("retrying") LifecycleJobStatus retrying,
            @Param("now") Instant now,
            Pageable pageable
    );

    boolean existsByEnvironmentIdAndStatusIn(
            UUID environmentId,
            Collection<LifecycleJobStatus> statuses
    );

    List<LifecycleJobEntity> findByStatusAndStartedAtBefore(
            LifecycleJobStatus status,
            Instant threshold
    );
}
