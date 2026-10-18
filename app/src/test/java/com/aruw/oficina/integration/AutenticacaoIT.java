package com.aruw.oficina.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutenticacaoIT extends IntegrationTestSupport {

    @Test
    void loginValidoRetornaToken() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", "admin", "password", "senha-de-teste"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void senhaInvalidaRetorna401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", "admin", "password", "errada"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiAdministrativaExigeToken() throws Exception {
        mvc.perform(get("/api/admin/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/clientes").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidoAcessaApiAdministrativa() throws Exception {
        getAdmin("/api/admin/servicos").andExpect(status().isOk());
    }

    @Test
    void documentacaoOpenApiEPublica() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Oficina Mecânica — API"));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
