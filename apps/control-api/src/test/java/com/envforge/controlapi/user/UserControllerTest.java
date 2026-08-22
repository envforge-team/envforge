package com.envforge.controlapi.user;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet
    .request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet
    .request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet
    .result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet
    .result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test
    .autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override
    .mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.envforge.controlapi.audit.AuditService;
import com.envforge.controlapi.security.AccessDeniedException;
import com.envforge.controlapi.security.AuthorizationService;
import com.envforge.controlapi.security.CurrentUser;
import com.envforge.controlapi.security.CurrentUserProvider;
import com.envforge.controlapi.security.SecurityMetrics;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private SecurityMetrics securityMetrics;

    @Test
    void shouldReturnCurrentUserProfile() throws Exception {
        CurrentUser currentUser = new CurrentUser(
            "dev-1",
            "dev-1@envforge.dev",
            "Dev One",
            Role.ADMIN
        );
        when(currentUserProvider.getCurrentUser())
            .thenReturn(currentUser);
        UserEntity user = new UserEntity(
            UUID.randomUUID(),
            "dev-1",
            "dev-1@envforge.dev",
            "Dev One",
            Role.ADMIN,
            Instant.now(),
            Instant.now()
        );
        when(userService.getOrCreateCurrentUser(currentUser))
            .thenReturn(user);

        mockMvc.perform(get("/api/me"))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.email")
                    .value("dev-1@envforge.dev")
            )
            .andExpect(
                jsonPath("$.role").value("ADMIN")
            );
    }

    @Test
    void shouldUpdateRoleAsAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        CurrentUser admin = new CurrentUser(
            "admin-1",
            "admin-1@envforge.dev",
            "Admin One",
            Role.ADMIN
        );
        when(currentUserProvider.getCurrentUser())
            .thenReturn(admin);
        UserEntity updated = new UserEntity(
            id,
            "target-1",
            "target-1@envforge.dev",
            "Target One",
            Role.OPERATOR,
            Instant.now(),
            Instant.now()
        );
        when(userService.updateRole(id, Role.OPERATOR))
            .thenReturn(updated);

        String request = """
            {
              "role": "OPERATOR"
            }
            """;

        mockMvc.perform(
                put("/api/users/{id}/role", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.role").value("OPERATOR")
            );
    }

    @Test
    void shouldForbidRoleUpdateForNonAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        CurrentUser operator = new CurrentUser(
            "operator-1",
            "operator-1@envforge.dev",
            "Operator One",
            Role.OPERATOR
        );
        when(currentUserProvider.getCurrentUser())
            .thenReturn(operator);
        org.mockito.Mockito.doThrow(
            new AccessDeniedException(
                "Role OPERATOR is not allowed to perform"
                    + " UPDATE_USER_ROLE"
            )
        ).when(authorizationService)
            .requireAdmin(operator, "UPDATE_USER_ROLE");

        String request = """
            {
              "role": "ADMIN"
            }
            """;

        mockMvc.perform(
                put("/api/users/{id}/role", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.status").value(403)
            );
    }

    @Test
    void shouldRejectInvalidRoleValue() throws Exception {
        UUID id = UUID.randomUUID();
        when(currentUserProvider.getCurrentUser())
            .thenReturn(
                new CurrentUser(
                    "admin-2",
                    "admin-2@envforge.dev",
                    "Admin Two",
                    Role.ADMIN
                )
            );

        String request = """
            {
              "role": "SUPERUSER"
            }
            """;

        mockMvc.perform(
                put("/api/users/{id}/role", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingUnknownUser()
        throws Exception {
        UUID id = UUID.randomUUID();
        when(currentUserProvider.getCurrentUser())
            .thenReturn(
                new CurrentUser(
                    "admin-3",
                    "admin-3@envforge.dev",
                    "Admin Three",
                    Role.ADMIN
                )
            );
        when(userService.updateRole(id, Role.OPERATOR))
            .thenThrow(new UserNotFoundException(id));

        String request = """
            {
              "role": "OPERATOR"
            }
            """;

        mockMvc.perform(
                put("/api/users/{id}/role", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.message")
                    .value("User not found: " + id)
            );
    }
}
