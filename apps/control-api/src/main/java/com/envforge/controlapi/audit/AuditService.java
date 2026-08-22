package com.envforge.controlapi.audit;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.envforge.controlapi.security.CurrentUser;
import com.envforge.controlapi.security.SecurityMetrics;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final SecurityMetrics securityMetrics;

    public AuditService(
        AuditEventRepository auditEventRepository,
        SecurityMetrics securityMetrics
    ) {
        this.auditEventRepository = auditEventRepository;
        this.securityMetrics = securityMetrics;
    }

    public void record(
        CurrentUser actor,
        String action,
        String resourceType,
        String resourceId,
        AuditResult result
    ) {
        record(actor, action, resourceType, resourceId, result, null);
    }

    public void record(
        CurrentUser actor,
        String action,
        String resourceType,
        String resourceId,
        AuditResult result,
        String details
    ) {
        AuditEventEntity event = new AuditEventEntity(
            UUID.randomUUID(),
            actor.email(),
            action,
            resourceType,
            resourceId,
            result,
            details,
            Instant.now()
        );
        auditEventRepository.save(event);
        securityMetrics.recordAuditEvent(result);
    }
}
