package com.aruw.oficina.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrdemServicoFluxoIT extends IntegrationTestSupport {

    private static final String TROCA_OLEO = "7b1e2f00-0000-4000-8000-000000000001";
    private static final String ALINHAMENTO = "7b1e2f00-0000-4000-8000-000000000002";
    private static final String OLEO = "8c2f3a00-0000-4000-8000-000000000001";
    private static final String FILTRO = "8c2f3a00-0000-4000-8000-000000000002";

    private Map<String, Object> novaOs(String cpf, String placa, int litrosOleo) {
        return Map.of(
                "cliente", Map.of("documento", cpf, "nome", "Roberto", "email", "roberto@email.com"),
                "veiculo", Map.of("placa", placa, "marca", "Honda", "modelo", "Civic", "ano", 2019),
                "descricaoProblema", "Revisão dos 30 mil km",
                "servicos", List.of(Map.of("servicoId", TROCA_OLEO)),
                "pecas", List.of(Map.of("pecaId", OLEO, "quantidade", litrosOleo)));
    }

    private int estoque(String pecaId) throws Exception {
        return ler(getAdmin("/api/admin/pecas/" + pecaId)).get("quantidadeEstoque").asInt();
    }

    @Test
    void fluxoCompletoComAprovacaoDoCliente() throws Exception {
        String cpf = cpfAleatorio();
        int estoqueOleoAntes = estoque(OLEO);

        JsonNode os = ler(postAdmin("/api/admin/ordens-servico", novaOs(cpf, placaAleatoria(), 4))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEBIDA"))
                .andExpect(jsonPath("$.cliente.nome").value("Roberto"))
                .andExpect(jsonPath("$.valorEstimado").value(260.0)));
        String id = os.get("id").asText();
        String base = "/api/admin/ordens-servico/" + id;

        postAdmin(base + "/diagnostico", null).andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));
        postAdmin(base + "/servicos", Map.of("servicoId", ALINHAMENTO)).andExpect(jsonPath("$.servicos", hasSize(2)));
        String itemFiltro = ler(postAdmin(base + "/pecas", Map.of("pecaId", FILTRO, "quantidade", 1))
                .andExpect(jsonPath("$.pecas", hasSize(2))))
                .get("pecas").get(1).get("id").asText();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete(base + "/pecas/" + itemFiltro).header("Authorization", "Bearer " + token()))
                .andExpect(jsonPath("$.pecas", hasSize(1)));

        postAdmin(base + "/orcamento", null)
                .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$.orcamento.situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.orcamento.valorServicos").value(200.0))
                .andExpect(jsonPath("$.orcamento.valorPecas").value(180.0))
                .andExpect(jsonPath("$.orcamento.valorTotal").value(380.0));

        // itens não podem mais ser alterados após o envio do orçamento
        postAdmin(base + "/servicos", Map.of("servicoId", ALINHAMENTO)).andExpect(status().isUnprocessableEntity());

        // cliente acompanha e aprova pela API pública
        mvc.perform(get("/api/public/ordens-servico/" + id).param("documento", cpf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusDescricao").value("Aguardando aprovação"));
        postPublico("/api/public/ordens-servico/" + id + "/orcamento/aprovacao", Map.of("documento", cpf))
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.orcamento.situacao").value("APROVADO"));

        org.assertj.core.api.Assertions.assertThat(estoque(OLEO)).isEqualTo(estoqueOleoAntes - 4);
        getAdmin("/api/admin/pecas/" + OLEO + "/movimentacoes").andExpect(jsonPath("$[0].tipo").value("SAIDA"));

        postAdmin(base + "/entrega", null).andExpect(status().isConflict());
        postAdmin(base + "/finalizacao", null).andExpect(jsonPath("$.status").value("FINALIZADA"));
        postAdmin(base + "/entrega", null)
                .andExpect(jsonPath("$.status").value("ENTREGUE"))
                .andExpect(jsonPath("$.historico", hasSize(6)));

        getAdmin(base).andExpect(jsonPath("$.tempoExecucaoMinutos").isNumber());
        getAdmin("/api/admin/ordens-servico?status=ENTREGUE").andExpect(status().isOk());
        getAdmin("/api/admin/ordens-servico").andExpect(status().isOk());
        getAdmin("/api/admin/ordens-servico/metricas/tempo-medio-execucao")
                .andExpect(jsonPath("$.ordensConsideradas").isNumber())
                .andExpect(jsonPath("$.porServico").isArray());
    }

    @Test
    void rejeicaoDoOrcamentoEncerraOsSemBaixarEstoque() throws Exception {
        String cpf = cpfAleatorio();
        String id = ler(postAdmin("/api/admin/ordens-servico", novaOs(cpf, placaAleatoria(), 1))).get("id").asText();
        String base = "/api/admin/ordens-servico/" + id;
        postAdmin(base + "/diagnostico", null);
        postAdmin(base + "/orcamento", null);
        int estoqueAntes = estoque(OLEO);

        postAdmin(base + "/orcamento/rejeicao", Map.of("motivo", "Vai fazer depois"))
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.orcamento.situacao").value("REJEITADO"));
        org.assertj.core.api.Assertions.assertThat(estoque(OLEO)).isEqualTo(estoqueAntes);
    }

    @Test
    void aprovacaoPelaOficinaERejeicaoPeloClienteJaDecidido() throws Exception {
        String cpf = cpfAleatorio();
        String id = ler(postAdmin("/api/admin/ordens-servico", novaOs(cpf, placaAleatoria(), 1))).get("id").asText();
        String base = "/api/admin/ordens-servico/" + id;
        postAdmin(base + "/diagnostico", null);
        postAdmin(base + "/orcamento", null);
        postAdmin(base + "/orcamento/aprovacao", null).andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
        postPublico("/api/public/ordens-servico/" + id + "/orcamento/rejeicao", Map.of("documento", cpf))
                .andExpect(status().isConflict());
    }

    @Test
    void clienteComOutroDocumentoNaoEnxergaOs() throws Exception {
        String id = ler(postAdmin("/api/admin/ordens-servico", novaOs(cpfAleatorio(), placaAleatoria(), 1))).get("id").asText();
        mvc.perform(get("/api/public/ordens-servico/" + id).param("documento", cpfAleatorio()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/public/ordens-servico/" + id).param("documento", "000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void naoAbreOsSemEstoqueOuComDadosInvalidos() throws Exception {
        postAdmin("/api/admin/ordens-servico", novaOs(cpfAleatorio(), placaAleatoria(), 100_000))
                .andExpect(status().isConflict());
        postAdmin("/api/admin/ordens-servico", novaOs("111.111.111-11", "XX", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos['cliente.documento']").exists())
                .andExpect(jsonPath("$.campos['veiculo.placa']").exists());
        getAdmin("/api/admin/ordens-servico/00000000-0000-0000-0000-000000000000").andExpect(status().isNotFound());
        getAdmin("/api/admin/ordens-servico/nao-e-uuid").andExpect(status().isBadRequest());
    }
}
