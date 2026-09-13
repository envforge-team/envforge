package com.envforge.controlapi.deployment;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentRepository;
import com.envforge.controlapi.environment.EnvironmentStatus;
import com.envforge.controlapi.environment.EnvironmentTemplate;
import com.envforge.controlapi.environment.ResourceProfile;
import com.envforge.controlapi.security.CurrentUser;
import com.envforge.controlapi.security.CurrentUserProvider;
import com.envforge.controlapi.user.Role;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(
    DeploymentConcurrencyIntegrationTest.TestConfig.class
)
class DeploymentConcurrencyIntegrationTest {

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private BlockingDeploymentExecutor blockingExecutor;

    private ExecutorService executorService;
    private UUID environmentId;

    @BeforeEach
    void setUp() {
        blockingExecutor.reset();

        executorService =
            Executors.newFixedThreadPool(2);

        environmentId = UUID.randomUUID();

        String suffix =
            environmentId
                .toString()
                .substring(0, 8);

        Instant now = Instant.now();

        EnvironmentEntity environment =
            new EnvironmentEntity(
                environmentId,
                "d34-" + suffix,
                "env-d34-" + suffix,
                EnvironmentTemplate.STATIC_WEB,
                "0.1.0",
                1,
                ResourceProfile.SMALL,
                EnvironmentStatus.READY,
                false,
                "concurrency-admin",
                now,
                now.plus(
                    4,
                    ChronoUnit.HOURS
                ),
                now
            );

        environmentRepository.saveAndFlush(
            environment
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        blockingExecutor.release();

        executorService.shutdown();

        executorService.awaitTermination(
            5,
            TimeUnit.SECONDS
        );

        List<DeploymentEntity> deployments =
            deploymentRepository
                .findByEnvironmentIdOrderByStartedAtDesc(
                    environmentId
                );

        deploymentRepository.deleteAll(
            deployments
        );

        deploymentRepository.flush();

        if (
            environmentRepository.existsById(
                environmentId
            )
        ) {
            environmentRepository.deleteById(
                environmentId
            );

            environmentRepository.flush();
        }
    }

    @Test
    void concurrentRolloutIsRejectedWhileFirstIsRunning()
        throws Exception {

        Future<DeploymentResponse> first =
            executorService.submit(
                () ->
                    deploymentService.triggerUpdate(
                        environmentId,
                        new UpdateEnvironmentRequest(
                            "0.2.0"
                        )
                    )
            );

        assertThat(
            blockingExecutor.awaitEntered(
                5,
                TimeUnit.SECONDS
            )
        ).isTrue();

        assertThat(
            blockingExecutor
                .transactionActiveWhenEntered()
        ).isFalse();

        List<DeploymentEntity> duringRollout =
            deploymentRepository
                .findByEnvironmentIdOrderByStartedAtDesc(
                    environmentId
                );

        assertThat(duringRollout)
            .hasSize(1);

        assertThat(
            duringRollout
                .getFirst()
                .getStatus()
        ).isEqualTo(
            DeploymentStatus.IN_PROGRESS
        );

        Future<DeploymentResponse> second =
            executorService.submit(
                () ->
                    deploymentService.triggerUpdate(
                        environmentId,
                        new UpdateEnvironmentRequest(
                            "0.3.0"
                        )
                    )
            );

        assertThatThrownBy(
            () -> second.get(
                5,
                TimeUnit.SECONDS
            )
        )
            .isInstanceOf(
                ExecutionException.class
            )
            .hasCauseInstanceOf(
                ConcurrentRolloutException.class
            );

        assertThat(
            blockingExecutor.calls()
        ).isEqualTo(1);

        blockingExecutor.release();

        DeploymentResponse firstResponse =
            first.get(
                5,
                TimeUnit.SECONDS
            );

        assertThat(
            firstResponse.status()
        ).isEqualTo(
            DeploymentStatus.SUCCESS
        );

        assertThat(
            firstResponse.requestedVersion()
        ).isEqualTo("0.2.0");

        assertThat(
            blockingExecutor.calls()
        ).isEqualTo(1);

        List<DeploymentEntity> finished =
            deploymentRepository
                .findByEnvironmentIdOrderByStartedAtDesc(
                    environmentId
                );

        assertThat(finished)
            .hasSize(1);

        assertThat(
            finished
                .getFirst()
                .getStatus()
        ).isEqualTo(
            DeploymentStatus.SUCCESS
        );

        EnvironmentEntity finalEnvironment =
            environmentRepository
                .findById(environmentId)
                .orElseThrow();

        assertThat(
            finalEnvironment.getStatus()
        ).isEqualTo(
            EnvironmentStatus.READY
        );

        assertThat(
            finalEnvironment.getImageVersion()
        ).isEqualTo("0.2.0");
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        BlockingDeploymentExecutor
            blockingDeploymentExecutor() {

            return new BlockingDeploymentExecutor();
        }

        @Bean
        @Primary
        CurrentUserProvider
            concurrencyCurrentUserProvider() {

            return () ->
                new CurrentUser(
                    "concurrency-admin",
                    "concurrency-admin@envforge.local",
                    "Concurrency Admin",
                    Role.ADMIN
                );
        }
    }

    static class BlockingDeploymentExecutor
        implements DeploymentExecutor {

        private final AtomicInteger calls =
            new AtomicInteger();

        private final AtomicBoolean
            transactionActive =
                new AtomicBoolean();

        private volatile CountDownLatch entered =
            new CountDownLatch(1);

        private volatile CountDownLatch release =
            new CountDownLatch(1);

        @Override
        public void deploy(
            EnvironmentEntity environment,
            String version
        ) {
            calls.incrementAndGet();

            transactionActive.set(
                TransactionSynchronizationManager
                    .isActualTransactionActive()
            );

            entered.countDown();

            try {
                boolean released =
                    release.await(
                        15,
                        TimeUnit.SECONDS
                    );

                if (!released) {
                    throw new IllegalStateException(
                        "Timed out waiting to release rollout"
                    );
                }
            } catch (
                InterruptedException exception
            ) {
                Thread.currentThread()
                    .interrupt();

                throw new IllegalStateException(
                    "Rollout interrupted",
                    exception
                );
            }
        }

        void reset() {
            calls.set(0);
            transactionActive.set(false);

            entered =
                new CountDownLatch(1);

            release =
                new CountDownLatch(1);
        }

        boolean awaitEntered(
            long timeout,
            TimeUnit unit
        ) throws InterruptedException {
            return entered.await(
                timeout,
                unit
            );
        }

        void release() {
            release.countDown();
        }

        int calls() {
            return calls.get();
        }

        boolean transactionActiveWhenEntered() {
            return transactionActive.get();
        }
    }
}
