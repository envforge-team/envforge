package com.envforge.cleanupworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CleanupWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleanupWorkerApplication.class, args);
    }
}
