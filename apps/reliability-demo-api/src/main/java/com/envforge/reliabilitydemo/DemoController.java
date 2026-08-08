package com.envforge.reliabilitydemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class DemoController {

    @Value("${app.version:0.1.0}")
    private String version;

    @Value("${app.force-not-ready:false}")
    private boolean forceNotReady;

    private final AtomicBoolean manuallyReady = new AtomicBoolean(true);
    private final AtomicLong requestCount = new AtomicLong(0);
    private final Instant startedAt = Instant.now();

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        requestCount.incrementAndGet();
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "uptimeSeconds", Duration.between(startedAt, Instant.now()).getSeconds()
        ));
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        requestCount.incrementAndGet();
        if (forceNotReady || !manuallyReady.get()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "DOWN", "reason", "not ready"));
        }
        return ResponseEntity.ok(Map.of("status", "READY"));
    }

    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> version() {
        requestCount.incrementAndGet();
        return ResponseEntity.ok(Map.of("version", version));
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        return ResponseEntity.ok(Map.of(
            "requestCount", requestCount.get(),
            "uptimeSeconds", Duration.between(startedAt, Instant.now()).getSeconds()
        ));
    }

    @PostMapping("/admin/ready")
    public ResponseEntity<Map<String, Object>> setReady(@RequestParam boolean value) {
        manuallyReady.set(value);
        return ResponseEntity.ok(Map.of("ready", manuallyReady.get()));
    }
}
