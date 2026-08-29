package com.envforge.cleanupworker.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.envforge.cleanupworker.domain.LifecycleAction;
import com.envforge.cleanupworker.service.LifecycleJobService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LifecycleJobControllerTest {

    @Test
    void shouldRejectMissingInternalToken() {
        LifecycleJobService jobService =
            mock(LifecycleJobService.class);

        LifecycleJobController controller =
            new LifecycleJobController(
                jobService,
                "expected-token"
            );

        LifecycleJobController.CreateLifecycleJobRequest request =
            new LifecycleJobController.CreateLifecycleJobRequest(
                UUID.randomUUID(),
                LifecycleAction.DELETE,
                null,
                "owner@example.test",
                "env-demo",
                "demo"
            );

        assertThatThrownBy(
            () -> controller.create(null, request)
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining(
                "Invalid internal lifecycle token"
            );
    }
}
