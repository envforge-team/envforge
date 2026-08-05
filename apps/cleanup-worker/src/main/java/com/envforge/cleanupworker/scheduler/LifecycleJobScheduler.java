package com.envforge.cleanupworker.scheduler;

import com.envforge.cleanupworker.config.LifecycleProperties;
import com.envforge.cleanupworker.domain.LifecycleJobStatus;
import com.envforge.cleanupworker.persistence.entity.LifecycleJobEntity;
import com.envforge.cleanupworker.persistence.repository.LifecycleJobRepository;
import com.envforge.cleanupworker.service.LifecycleJobProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(
        prefix = "envforge.lifecycle",
        name = "scheduler-enabled",
        havingValue = "true"
)
public class LifecycleJobScheduler {

    private final LifecycleJobRepository jobRepository;
    private final LifecycleJobProcessor jobProcessor;
    private final LifecycleProperties properties;

    public LifecycleJobScheduler(
            LifecycleJobRepository jobRepository,
            LifecycleJobProcessor jobProcessor,
            LifecycleProperties properties
    ) {
        this.jobRepository = jobRepository;
        this.jobProcessor = jobProcessor;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString =
                    "${envforge.lifecycle.scheduler-delay-milliseconds}"
    )
    @Transactional
    public void processReadyJobs() {
        List<LifecycleJobEntity> jobs =
                jobRepository.findReadyJobsForUpdate(
                        LifecycleJobStatus.QUEUED,
                        LifecycleJobStatus.RETRYING,
                        Instant.now(),
                        PageRequest.of(0, properties.batchSize())
                );

        for (LifecycleJobEntity job : jobs) {
            jobProcessor.process(job);
        }
    }
}
