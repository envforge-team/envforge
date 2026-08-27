package com.envforge.cleanupworker.scheduler;

import com.envforge.cleanupworker.config.LifecycleProperties;
import com.envforge.cleanupworker.domain.LifecycleJobStatus;
import com.envforge.cleanupworker.persistence.entity.LifecycleJobEntity;
import com.envforge.cleanupworker.persistence.repository.LifecycleJobRepository;
import com.envforge.cleanupworker.service.LifecycleJobProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        havingValue = "true",
        matchIfMissing = false
)
public class LifecycleJobScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(LifecycleJobScheduler.class);

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
                    "${envforge.lifecycle.scheduler-delay-milliseconds:10000}"
    )
    @Transactional
    public void processReadyJobs() {
        Instant now = Instant.now();

        log.info(
                "Lifecycle scheduler tick: now={}, batchSize={}",
                now,
                properties.batchSize()
        );

        try {
            List<LifecycleJobEntity> jobs =
                    jobRepository.findReadyJobsForUpdate(
                            LifecycleJobStatus.QUEUED,
                            LifecycleJobStatus.RETRYING,
                            now,
                            PageRequest.of(
                                    0,
                                    properties.batchSize()
                            )
                    );

            log.info(
                    "Lifecycle scheduler found {} ready job(s)",
                    jobs.size()
            );

            for (LifecycleJobEntity job : jobs) {
                log.info(
                        "Processing lifecycle job id={}, environmentId={}, action={}, status={}, attemptCount={}",
                        job.getId(),
                        job.getEnvironmentId(),
                        job.getAction(),
                        job.getStatus(),
                        job.getAttemptCount()
                );

                try {
                    jobProcessor.process(job);

                    log.info(
                            "Finished lifecycle job id={}, resultingStatus={}",
                            job.getId(),
                            job.getStatus()
                    );
                } catch (RuntimeException exception) {
                    log.error(
                            "Lifecycle job processor failed for job id={}",
                            job.getId(),
                            exception
                    );
                }
            }
        } catch (RuntimeException exception) {
            log.error(
                    "Lifecycle scheduler failed while loading ready jobs",
                    exception
            );
        }
    }
}
