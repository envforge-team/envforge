package com.envforge.controlapi.template;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet
    .request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet
    .result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet
    .result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.envforge.controlapi.environment
    .EnvironmentTemplate;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test
    .autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override
    .mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TemplateController.class)
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TemplateService templateService;

    @Test
    void shouldReturnActiveTemplates() throws Exception {
        TemplateResponse reliabilityApi =
            new TemplateResponse(
                UUID.fromString(
                    "22222222-2222-2222-2222-222222222222"
                ),
                EnvironmentTemplate.RELIABILITY_API,
                "Reliability Demo API",
                "envforge/reliability-demo-api",
                "0.1.0"
            );

        TemplateResponse staticWeb =
            new TemplateResponse(
                UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
                ),
                EnvironmentTemplate.STATIC_WEB,
                "Static Web App",
                "envforge/static-web-demo",
                "0.1.0"
            );

        when(templateService.findActiveTemplates())
            .thenReturn(
                List.of(reliabilityApi, staticWeb)
            );

        mockMvc.perform(get("/api/templates"))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.length()").value(2)
            )
            .andExpect(
                jsonPath("$[0].code")
                    .value("RELIABILITY_API")
            )
            .andExpect(
                jsonPath("$[0].displayName")
                    .value("Reliability Demo API")
            )
            .andExpect(
                jsonPath("$[1].code")
                    .value("STATIC_WEB")
            )
            .andExpect(
                jsonPath("$[1].defaultImageVersion")
                    .value("0.1.0")
            );
    }

    @Test
    void shouldReturnEmptyTemplateList()
        throws Exception {

        when(templateService.findActiveTemplates())
            .thenReturn(List.of());

        mockMvc.perform(get("/api/templates"))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.length()").value(0)
            );
    }
}