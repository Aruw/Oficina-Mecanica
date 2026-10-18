package com.aruw.oficina.api.ordemservico;

import com.aruw.oficina.api.validation.CpfOuCnpj;
import com.aruw.oficina.api.validation.PlacaValida;
import com.aruw.oficina.application.ordemservico.AbrirOrdemServicoCommand;
import com.aruw.oficina.application.ordemservico.MetricasService;
import com.aruw.oficina.application.ordemservico.OrdemServicoDetalhe;
import com.aruw.oficina.application.ordemservico.OrdemServicoService;
import com.aruw.oficina.application.ordemservico.TempoMedioExecucao;
import com.aruw.oficina.domain.ordemservico.StatusOrdemServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/ordens-servico")
@Tag(name = "Ordens de Serviço", description = "Abertura, itens, orçamento e ciclo de vida da OS")
@SecurityRequirement(name = "bearerAuth")
public class OrdemServicoController {

    private final OrdemServicoService service;
    private final MetricasService metricas;

    public OrdemServicoController(OrdemServicoService service, MetricasService metricas) {
        this.service = service;
        this.metricas = metricas;
    }

    public record ClienteOsRequest(
            @NotBlank @CpfOuCnpj String documento,
            @Size(max = 150) String nome,
            @Email @Size(max = 150) String email,
            @Size(max = 20) String telefone) {
    }

    public record VeiculoOsRequest(
            @NotBlank @PlacaValida String placa,
            @Size(max = 60) String marca,
            @Size(max = 80) String modelo,
            @Min(1900) @Max(2100) Integer ano) {
    }

    public record ItemServicoRequest(@NotNull UUID servicoId, @Min(1) Integer quantidade) {
        int quantidadeOuUm() {
            return quantidade == null ? 1 : quantidade;
        }
    }

    public record ItemPecaRequest(@NotNull UUID pecaId, @NotNull @Min(1) Integer quantidade) {
    }

    public record AbrirOrdemServicoRequest(
            @NotNull @Valid ClienteOsRequest cliente,
            @NotNull @Valid VeiculoOsRequest veiculo,
            @Size(max = 1000) String descricaoProblema,
            List<@Valid ItemServicoRequest> servicos,
            List<@Valid ItemPecaRequest> pecas) {

        AbrirOrdemServicoCommand toCommand() {
            return new AbrirOrdemServicoCommand(
                    new AbrirOrdemServicoCommand.Cliente(cliente.documento(), cliente.nome(), cliente.email(), cliente.telefone()),
                    new AbrirOrdemServicoCommand.Veiculo(veiculo.placa(), veiculo.marca(), veiculo.modelo(), veiculo.ano()),
                    descricaoProblema,
                    servicos == null ? List.of() : servicos.stream()
                            .map(s -> new AbrirOrdemServicoCommand.ItemServico(s.servicoId(), s.quantidadeOuUm())).toList(),
                    pecas == null ? List.of() : pecas.stream()
                            .map(p -> new AbrirOrdemServicoCommand.ItemPeca(p.pecaId(), p.quantidade())).toList());
        }
    }

    public record RejeicaoRequest(@Size(max = 500) String motivo) {
    }

    @PostMapping
    @Operation(summary = "Abre OS: identifica cliente por CPF/CNPJ, cadastra veículo se necessário e inclui serviços/peças")
    public ResponseEntity<OrdemServicoDetalhe> abrir(@Valid @RequestBody AbrirOrdemServicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.abrir(request.toCommand()));
    }

    @GetMapping
    @Operation(summary = "Lista OS ordenadas por prioridade de atendimento (filtro opcional por status)")
    public List<OrdemServicoDetalhe> listar(@RequestParam(required = false) StatusOrdemServico status) {
        return service.listar(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha OS com itens, orçamento e histórico de status")
    public OrdemServicoDetalhe detalhar(@PathVariable UUID id) {
        return service.detalhar(id);
    }

    @PostMapping("/{id}/servicos")
    @Operation(summary = "Inclui serviço na OS (Recebida ou Em diagnóstico)")
    public OrdemServicoDetalhe adicionarServico(@PathVariable UUID id, @Valid @RequestBody ItemServicoRequest request) {
        return service.adicionarServico(id, request.servicoId(), request.quantidadeOuUm());
    }

    @DeleteMapping("/{id}/servicos/{itemId}")
    @Operation(summary = "Remove serviço da OS")
    public OrdemServicoDetalhe removerServico(@PathVariable UUID id, @PathVariable UUID itemId) {
        return service.removerServico(id, itemId);
    }

    @PostMapping("/{id}/pecas")
    @Operation(summary = "Inclui peça ou insumo na OS (verifica disponibilidade em estoque)")
    public OrdemServicoDetalhe adicionarPeca(@PathVariable UUID id, @Valid @RequestBody ItemPecaRequest request) {
        return service.adicionarPeca(id, request.pecaId(), request.quantidade());
    }

    @DeleteMapping("/{id}/pecas/{itemId}")
    @Operation(summary = "Remove peça ou insumo da OS")
    public OrdemServicoDetalhe removerPeca(@PathVariable UUID id, @PathVariable UUID itemId) {
        return service.removerPeca(id, itemId);
    }

    @PostMapping("/{id}/diagnostico")
    @Operation(summary = "Inicia o diagnóstico (Recebida → Em diagnóstico)")
    public OrdemServicoDetalhe iniciarDiagnostico(@PathVariable UUID id) {
        return service.iniciarDiagnostico(id);
    }

    @PostMapping("/{id}/orcamento")
    @Operation(summary = "Gera o orçamento e envia ao cliente (Em diagnóstico → Aguardando aprovação)")
    public OrdemServicoDetalhe gerarOrcamento(@PathVariable UUID id) {
        return service.gerarOrcamento(id);
    }

    @PostMapping("/{id}/orcamento/aprovacao")
    @Operation(summary = "Registra a aprovação do orçamento e baixa o estoque (→ Em execução)")
    public OrdemServicoDetalhe aprovarOrcamento(@PathVariable UUID id) {
        return service.aprovarOrcamento(id);
    }

    @PostMapping("/{id}/orcamento/rejeicao")
    @Operation(summary = "Registra a rejeição do orçamento (→ Finalizada, sem execução)")
    public OrdemServicoDetalhe rejeitarOrcamento(@PathVariable UUID id, @Valid @RequestBody(required = false) RejeicaoRequest request) {
        return service.rejeitarOrcamento(id, request == null ? null : request.motivo());
    }

    @PostMapping("/{id}/finalizacao")
    @Operation(summary = "Finaliza a execução (Em execução → Finalizada)")
    public OrdemServicoDetalhe finalizar(@PathVariable UUID id) {
        return service.finalizar(id);
    }

    @PostMapping("/{id}/entrega")
    @Operation(summary = "Registra a entrega do veículo (Finalizada → Entregue)")
    public OrdemServicoDetalhe entregar(@PathVariable UUID id) {
        return service.entregar(id);
    }

    @GetMapping("/metricas/tempo-medio-execucao")
    @Operation(summary = "Tempo médio de execução das OS (geral e por serviço)")
    public TempoMedioExecucao tempoMedioExecucao() {
        return metricas.tempoMedioExecucao();
    }
}
