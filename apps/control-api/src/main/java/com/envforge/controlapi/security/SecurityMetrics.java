package com.envforge.controlapi.security;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import com.envforge.controlapi.audit.AuditResult;

/**
 * Central place to record security-related Micrometer metrics: logins,
 * authorization denials (HTTP 401/403), and audit events. Exposed via
 * /actuator/prometheus for dashboards and alerting (Ziua 30).
 *
 * The 401 counter is not wired to any code path yet, because
 * SecurityConfig is still permissive (Saptamana 2 placeholder). It is
 * ready to be called once real authentication (Entra ID / JWT,
 * Saptamana 7) can actually produce a 401 response.
 */
@Component
public class SecurityMetrics {

    private final MeterRegistry meterRegistry;

    public SecurityMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordLogin() {
        meterRegistry.counter("envforge.security.logins").increment();
    }

    public void recordUnauthorized() {
        meterRegistry.counter(
            "envforge.security.http.denied",
            "status", "401"
        ).increment();
    }

    public void recordForbidden() {
        meterRegistry.counter(
            "envforge.security.http.denied",
            "status", "403"
        ).increment();
    }

    public void recordAuditEvent(AuditResult result) {
        meterRegistry.counter(
            "envforge.security.audit.events",
            "result", result.name()
        ).increment();
    }
}
