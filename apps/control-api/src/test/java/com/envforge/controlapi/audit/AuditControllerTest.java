package com.envforge.controlapi.audit;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet
    .request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet
    .result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet
    .result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test
    .autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override
    .mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.envforge.controlapi.security.AccessDeniedException;
import com.envforge.controlapi.security.AuthorizationService;
import com.envforge.controlapi.security.CurrentUser;
import com.envforge.controlapi.security.CurrentUserProvider;
import com.envforge.controlapi.user.Role;
@WebMvcTest(AuditController.class)
class AuditControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AuditEventRepository auditEventRepository;
    @MockitoBean
    private CurrentUserProvider currentUserProvider;
    @MockitoBean
    private AuthorizationService authorizationService;
    @Test
    void shouldListAuditEventsForAdmin() throws Exception {
        CurrentUser admin = new CurrentUser(
            "admin-1",
            "admin-1@envforge.dev",
            "Admin One",
            Role.ADMIN
        );
        when(currentUserProvider.getCurrentUser())
            .thenReturn(admin);
        AuditEventEntity event = new AuditEventEntity(
            UUID.randomUUID(),
            "someone@envforge.dev",
            "UPDATE_USER_ROLE",
            "USER",
            UUID.randomUUID().toString(),
            AuditResult.SUCCESS,
            "Role changed to OPERATOR",
            Instant.now()
        );
        when(
            auditEventRepository.findAllByOrderByCreatedAtDesc()
        ).thenReturn(List.of(event));
        mockMvc.perform(get("/api/audit"))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.length()").value(1)
            )
            .andExpect(
                jsonPath("$[0].action")
                    .value("UPDATE_USER_ROLE")
            );
    }
    @Test
    void shouldForbidAuditLogForNonAdmin() throws Exception {
        CurrentUser user = new CurrentUser(
            "user-1",
            "user-1@envforge.dev",
            "User One",
            Role.USER
        );
        when(currentUserProvider.getCurrentUser())
            .thenReturn(user);
        org.mockito.Mockito.doThrow(
            new AccessDeniedException(
                "Role USER is not allowed to perform"
                    + " VIEW_AUDIT_LOG"
            )
        ).when(authorizationService)
            .requireAdmin(user, "VIEW_AUDIT_LOG");
        mockMvc.perform(get("/api/audit"))
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.status").value(403)
            );
    }
}
