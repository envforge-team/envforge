package com.envforge.reliabilitydemo;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IncidentController {

    private static final long MAX_LATENCY_MS = 5_000;
    private static final long MAX_CPU_LOAD_MS = 10_000;

    private final AtomicBoolean failureEnabled =
        new AtomicBoolean(false);

    private final AtomicLong latencyMillis =
        new AtomicLong(0);

    @GetMapping("/work")
    public ResponseEntity<Map<String, Object>> work()
        throws InterruptedException {

        long configuredLatency = latencyMillis.get();

        if (configuredLatency > 0) {
            Thread.sleep(configuredLatency);
        }

        if (failureEnabled.get()) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    Map.of(
                        "status", "ERROR",
                        "message",
                        "Controlled 5xx incident is enabled"
                    )
                );
        }

        return ResponseEntity.ok(
            Map.of(
                "status", "OK",
                "latencyMillis", configuredLatency
            )
        );
    }

    @PostMapping("/admin/incidents/failure")
    public ResponseEntity<Map<String, Object>> setFailure(
        @RequestParam boolean enabled
    ) {
        failureEnabled.set(enabled);

        return ResponseEntity.ok(
            Map.of(
                "failureEnabled",
                failureEnabled.get()
            )
        );
    }

    @PostMapping("/admin/incidents/latency")
    public ResponseEntity<Map<String, Object>> setLatency(
        @RequestParam long milliseconds
    ) {
        if (
            milliseconds < 0 ||
            milliseconds > MAX_LATENCY_MS
        ) {
            return ResponseEntity
                .badRequest()
                .body(
                    Map.of(
                        "error",
                        "milliseconds must be between 0 and "
                            + MAX_LATENCY_MS
                    )
                );
        }

        latencyMillis.set(milliseconds);

        return ResponseEntity.ok(
            Map.of(
                "latencyMillis",
                latencyMillis.get()
            )
        );
    }

    @PostMapping("/admin/incidents/cpu")
    public ResponseEntity<Map<String, Object>> generateCpuLoad(
        @RequestParam long milliseconds
    ) {
        if (
            milliseconds < 1 ||
            milliseconds > MAX_CPU_LOAD_MS
        ) {
            return ResponseEntity
                .badRequest()
                .body(
                    Map.of(
                        "error",
                        "milliseconds must be between 1 and "
                            + MAX_CPU_LOAD_MS
                    )
                );
        }

        long deadline =
            System.nanoTime()
                + milliseconds * 1_000_000L;

        long iterations = 0;

        while (System.nanoTime() < deadline) {
            iterations++;
            Math.sqrt(iterations);
        }

        return ResponseEntity.ok(
            Map.of(
                "status", "COMPLETED",
                "durationMillis", milliseconds,
                "iterations", iterations
            )
        );
    }

    @PostMapping("/admin/incidents/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        failureEnabled.set(false);
        latencyMillis.set(0);

        return ResponseEntity.ok(
            Map.of(
                "failureEnabled", false,
                "latencyMillis", 0
            )
        );
    }

    @GetMapping("/admin/incidents/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(
            Map.of(
                "failureEnabled",
                failureEnabled.get(),
                "latencyMillis",
                latencyMillis.get()
            )
        );
    }
}
