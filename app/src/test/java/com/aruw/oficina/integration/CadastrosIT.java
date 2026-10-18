package com.aruw.oficina.integration;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CadastrosIT extends IntegrationTestSupport {

    @Test
    void crudDeCliente() throws Exception {
        String cpf = cpfAleatorio();
        String id = ler(postAdmin("/api/admin/clientes", Map.of("nome", "Carla", "documento", cpf, "email", "carla@email.com"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoDocumento").value("CPF")))
                .get("id").asText();

        getAdmin("/api/admin/clientes/" + id).andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Carla"));
        getAdmin("/api/admin/clientes?documento=" + cpf).andExpect(jsonPath("$", hasSize(1)));
        putAdmin("/api/admin/clientes/" + id, Map.of("nome", "Carla Souza", "documento", cpf))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Carla Souza"));
        getAdmin("/api/admin/clientes").andExpect(status().isOk());

        postAdmin("/api/admin/clientes", Map.of("nome", "Dup", "documento", cpf)).andExpect(status().isUnprocessableEntity());

        deleteAdmin("/api/admin/clientes/" + id).andExpect(status().isNoContent());
        getAdmin("/api/admin/clientes/" + id).andExpect(status().isNotFound());
    }

    @Test
    void validaCpfCnpjEPlaca() throws Exception {
        postAdmin("/api/admin/clientes", Map.of("nome", "X", "documento", "123.456.789-00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.documento").value("CPF/CNPJ inválido"));

        postAdmin("/api/admin/clientes", Map.of("nome", "Empresa", "documento", "12.ABC.345/01DE-35"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoDocumento").value("CNPJ"));

        Map<String, Object> veiculo = new HashMap<>(Map.of("placa", "AB-12", "marca", "Fiat", "modelo", "Uno", "ano", 2010,
                "clienteId", "00000000-0000-0000-0000-000000000000"));
        postAdmin("/api/admin/veiculos", veiculo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.placa").exists());
    }

    @Test
    void crudDeVeiculo() throws Exception {
        String clienteId = ler(postAdmin("/api/admin/clientes", Map.of("nome", "Paulo", "documento", cpfAleatorio())))
                .get("id").asText();
        String placa = placaAleatoria();
        String id = ler(postAdmin("/api/admin/veiculos",
                Map.of("placa", placa, "marca", "Toyota", "modelo", "Corolla", "ano", 2022, "clienteId", clienteId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mercosul").value(true)))
                .get("id").asText();

        getAdmin("/api/admin/veiculos?clienteId=" + clienteId).andExpect(jsonPath("$", hasSize(1)));
        putAdmin("/api/admin/veiculos/" + id,
                Map.of("placa", placa, "marca", "Toyota", "modelo", "Corolla XEi", "ano", 2022, "clienteId", clienteId))
                .andExpect(jsonPath("$.modelo").value("Corolla XEi"));
        getAdmin("/api/admin/veiculos/" + id).andExpect(status().isOk());

        deleteAdmin("/api/admin/clientes/" + clienteId).andExpect(status().isUnprocessableEntity());
        deleteAdmin("/api/admin/veiculos/" + id).andExpect(status().isNoContent());
        getAdmin("/api/admin/veiculos").andExpect(status().isOk());
    }

    @Test
    void crudDeServico() throws Exception {
        String id = ler(postAdmin("/api/admin/servicos",
                Map.of("nome", "Higienização", "preco", 90.5, "tempoEstimadoMinutos", 45))
                .andExpect(status().isCreated()))
                .get("id").asText();
        putAdmin("/api/admin/servicos/" + id,
                Map.of("nome", "Higienização A/C", "preco", 95, "tempoEstimadoMinutos", 45, "ativo", false))
                .andExpect(jsonPath("$.ativo").value(false));
        getAdmin("/api/admin/servicos/" + id).andExpect(jsonPath("$.preco").value(95.0));
        deleteAdmin("/api/admin/servicos/" + id).andExpect(status().isNoContent());

        postAdmin("/api/admin/servicos", Map.of("nome", "", "preco", -1, "tempoEstimadoMinutos", 0))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crudDePecaComControleDeEstoque() throws Exception {
        String codigo = "TEST-" + System.nanoTime();
        String id = ler(postAdmin("/api/admin/pecas", Map.of("codigo", codigo, "nome", "Vela", "tipo", "PECA",
                "precoUnitario", 25, "quantidadeEstoque", 4, "estoqueMinimo", 5))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.abaixoDoMinimo").value(true)))
                .get("id").asText();

        postAdmin("/api/admin/pecas/" + id + "/entradas", Map.of("quantidade", 10, "motivo", "Compra NF 123"))
                .andExpect(jsonPath("$.quantidadeEstoque").value(14))
                .andExpect(jsonPath("$.abaixoDoMinimo").value(false));
        getAdmin("/api/admin/pecas/" + id + "/movimentacoes").andExpect(jsonPath("$", hasSize(2)));
        putAdmin("/api/admin/pecas/" + id, Map.of("codigo", codigo, "nome", "Vela de ignição", "tipo", "PECA",
                "precoUnitario", 30, "quantidadeEstoque", 0, "estoqueMinimo", 2))
                .andExpect(jsonPath("$.nome").value("Vela de ignição"))
                .andExpect(jsonPath("$.quantidadeEstoque").value(14));
        getAdmin("/api/admin/pecas?abaixoDoMinimo=true").andExpect(status().isOk());
        getAdmin("/api/admin/pecas/" + id).andExpect(status().isOk());

        postAdmin("/api/admin/pecas/" + id + "/entradas", Map.of("quantidade", 0)).andExpect(status().isBadRequest());
        deleteAdmin("/api/admin/pecas/" + id).andExpect(status().isNoContent());
    }
}
