package com.envforge.controlapi.lifecycle;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LifecycleController.class)
class LifecycleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LifecycleService lifecycleService;

    @Test
    void shouldAcceptDeleteRequest() throws Exception {
        UUID environmentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(lifecycleService.delete(environmentId))
            .thenReturn(
                new LifecycleJobResponse(
                    jobId,
                    environmentId,
                    "DELETE",
                    "QUEUED",
                    0
                )
            );

        mockMvc.perform(
                post(
                    "/api/environments/{id}/delete",
                    environmentId
                )
            )
            .andExpect(status().isAccepted())
            .andExpect(
                jsonPath("$.id")
                    .value(jobId.toString())
            )
            .andExpect(
                jsonPath("$.action")
                    .value("DELETE")
            );
    }

    @Test
    void shouldValidateRollbackRevision() throws Exception {
        UUID environmentId = UUID.randomUUID();

        mockMvc.perform(
                post(
                    "/api/environments/{id}/rollback",
                    environmentId
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "targetRevision": 0
                        }
                        """
                    )
            )
            .andExpect(status().isBadRequest());
    }
}
