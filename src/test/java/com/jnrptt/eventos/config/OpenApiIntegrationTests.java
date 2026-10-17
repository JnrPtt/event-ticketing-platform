package com.jnrptt.eventos.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsDeclaraElEsquemaBearerSoloEnLosEndpointsProtegidos() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/compras'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/eventos'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/eventos'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/auth/login'].post.security").doesNotExist());
    }
}
