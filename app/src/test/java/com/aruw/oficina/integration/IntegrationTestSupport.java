package com.aruw.oficina.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    private String token;

    protected String token() throws Exception {
        if (token == null) {
            String body = mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("username", "admin", "password", "senha-de-teste"))))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            token = json.readTree(body).get("accessToken").asText();
        }
        return token;
    }

    protected ResultActions getAdmin(String url) throws Exception {
        return mvc.perform(autenticado(get(url)));
    }

    protected ResultActions postAdmin(String url, Object body) throws Exception {
        return mvc.perform(comCorpo(autenticado(post(url)), body));
    }

    protected ResultActions putAdmin(String url, Object body) throws Exception {
        return mvc.perform(comCorpo(autenticado(put(url)), body));
    }

    protected ResultActions deleteAdmin(String url) throws Exception {
        return mvc.perform(autenticado(delete(url)));
    }

    protected ResultActions postPublico(String url, Object body) throws Exception {
        return mvc.perform(comCorpo(post(url), body));
    }

    protected JsonNode ler(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder req) throws Exception {
        return req.header("Authorization", "Bearer " + token());
    }

    private MockHttpServletRequestBuilder comCorpo(MockHttpServletRequestBuilder req, Object body) throws Exception {
        req.contentType(MediaType.APPLICATION_JSON);
        return body == null ? req : req.content(json.writeValueAsString(body));
    }

    protected static String cpfAleatorio() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder base = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            base.append(r.nextInt(10));
        }
        int d1 = digito(base.toString(), 10);
        int d2 = digito(base.toString() + d1, 11);
        return base.toString() + d1 + d2;
    }

    private static int digito(String base, int peso) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += (base.charAt(i) - '0') * (peso - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    protected static String placaAleatoria() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return "" + (char) ('A' + r.nextInt(26)) + (char) ('A' + r.nextInt(26)) + (char) ('A' + r.nextInt(26))
                + r.nextInt(10) + (char) ('A' + r.nextInt(26)) + r.nextInt(10) + r.nextInt(10);
    }
}
