package com.envforge.controlapi.audit;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.envforge.controlapi.security.AuthorizationService;
import com.envforge.controlapi.security.CurrentUser;
import com.envforge.controlapi.security.CurrentUserProvider;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditEventRepository auditEventRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    public AuditController(
        AuditEventRepository auditEventRepository,
        CurrentUserProvider currentUserProvider,
        AuthorizationService authorizationService
    ) {
        this.auditEventRepository = auditEventRepository;
        this.currentUserProvider = currentUserProvider;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<List<AuditEventResponse>> list() {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        authorizationService.requireAdmin(currentUser, "VIEW_AUDIT_LOG");

        List<AuditEventResponse> events = auditEventRepository
            .findAllByOrderByCreatedAtDesc()
            .stream()
            .map(AuditEventResponse::from)
            .toList();

        return ResponseEntity.ok(events);
    }
}
