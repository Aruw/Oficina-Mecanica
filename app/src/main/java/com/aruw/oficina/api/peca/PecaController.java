package com.aruw.oficina.api.peca;

import com.aruw.oficina.application.estoque.PecaCommand;
import com.aruw.oficina.application.estoque.PecaService;
import com.aruw.oficina.domain.estoque.MovimentacaoEstoque;
import com.aruw.oficina.domain.estoque.Peca;
import com.aruw.oficina.domain.estoque.TipoItemEstoque;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/pecas")
@Tag(name = "Peças e insumos", description = "CRUD de peças/insumos e controle de estoque")
@SecurityRequirement(name = "bearerAuth")
public class PecaController {

    private final PecaService service;

    public PecaController(PecaService service) {
        this.service = service;
    }

    public record PecaRequest(
            @NotBlank @Size(max = 40) String codigo,
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 500) String descricao,
            @NotNull TipoItemEstoque tipo,
            @NotNull @DecimalMin("0.00") BigDecimal precoUnitario,
            @Min(0) int quantidadeEstoque,
            @Min(0) int estoqueMinimo) {

        PecaCommand toCommand() {
            return new PecaCommand(codigo, nome, descricao, tipo, precoUnitario, quantidadeEstoque, estoqueMinimo);
        }
    }

    public record EntradaEstoqueRequest(@NotNull @Min(1) Integer quantidade, @Size(max = 255) String motivo) {
    }

    public record PecaResponse(UUID id, String codigo, String nome, String descricao, TipoItemEstoque tipo,
                               BigDecimal precoUnitario, int quantidadeEstoque, int estoqueMinimo, boolean abaixoDoMinimo) {
        static PecaResponse de(Peca p) {
            return new PecaResponse(p.getId(), p.getCodigo(), p.getNome(), p.getDescricao(), p.getTipo(),
                    p.getPrecoUnitario(), p.getQuantidadeEstoque(), p.getEstoqueMinimo(), p.abaixoDoMinimo());
        }
    }

    public record MovimentacaoResponse(UUID id, MovimentacaoEstoque.Tipo tipo, int quantidade, String motivo,
                                       UUID ordemServicoId, Instant dataHora) {
        static MovimentacaoResponse de(MovimentacaoEstoque m) {
            return new MovimentacaoResponse(m.getId(), m.getTipo(), m.getQuantidade(), m.getMotivo(),
                    m.getOrdemServicoId(), m.getDataHora());
        }
    }

    @PostMapping
    @Operation(summary = "Cadastra peça ou insumo com saldo inicial")
    public ResponseEntity<PecaResponse> criar(@Valid @RequestBody PecaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(PecaResponse.de(service.criar(request.toCommand())));
    }

    @GetMapping
    @Operation(summary = "Lista peças e insumos (opcionalmente apenas os abaixo do estoque mínimo)")
    public List<PecaResponse> listar(@RequestParam(defaultValue = "false") boolean abaixoDoMinimo) {
        return service.listar(abaixoDoMinimo).stream().map(PecaResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha peça ou insumo")
    public PecaResponse buscar(@PathVariable UUID id) {
        return PecaResponse.de(service.buscar(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados cadastrais (o saldo muda apenas por movimentações)")
    public PecaResponse atualizar(@PathVariable UUID id, @Valid @RequestBody PecaRequest request) {
        return PecaResponse.de(service.atualizar(id, request.toCommand()));
    }

    @PostMapping("/{id}/entradas")
    @Operation(summary = "Registra entrada (reposição) de estoque")
    public PecaResponse registrarEntrada(@PathVariable UUID id, @Valid @RequestBody EntradaEstoqueRequest request) {
        return PecaResponse.de(service.registrarEntrada(id, request.quantidade(), request.motivo()));
    }

    @GetMapping("/{id}/movimentacoes")
    @Operation(summary = "Histórico de movimentações de estoque do item")
    public List<MovimentacaoResponse> movimentacoes(@PathVariable UUID id) {
        return service.movimentacoes(id).stream().map(MovimentacaoResponse::de).toList();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove peça ou insumo (perfil ADMIN)")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
