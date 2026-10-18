package com.aruw.oficina.api.servico;

import com.aruw.oficina.application.catalogo.ServicoCommand;
import com.aruw.oficina.application.catalogo.ServicoService;
import com.aruw.oficina.domain.catalogo.Servico;
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
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/servicos")
@Tag(name = "Serviços", description = "CRUD do catálogo de serviços")
@SecurityRequirement(name = "bearerAuth")
public class ServicoController {

    private final ServicoService service;

    public ServicoController(ServicoService service) {
        this.service = service;
    }

    public record ServicoRequest(
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 500) String descricao,
            @NotNull @DecimalMin("0.00") BigDecimal preco,
            @NotNull @Min(1) Integer tempoEstimadoMinutos,
            Boolean ativo) {

        ServicoCommand toCommand() {
            return new ServicoCommand(nome, descricao, preco, tempoEstimadoMinutos, ativo);
        }
    }

    public record ServicoResponse(UUID id, String nome, String descricao, BigDecimal preco, int tempoEstimadoMinutos,
                                  boolean ativo) {
        static ServicoResponse de(Servico s) {
            return new ServicoResponse(s.getId(), s.getNome(), s.getDescricao(), s.getPreco(),
                    s.getTempoEstimadoMinutos(), s.isAtivo());
        }
    }

    @PostMapping
    @Operation(summary = "Cadastra serviço")
    public ResponseEntity<ServicoResponse> criar(@Valid @RequestBody ServicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ServicoResponse.de(service.criar(request.toCommand())));
    }

    @GetMapping
    @Operation(summary = "Lista serviços")
    public List<ServicoResponse> listar() {
        return service.listar().stream().map(ServicoResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha serviço")
    public ServicoResponse buscar(@PathVariable UUID id) {
        return ServicoResponse.de(service.buscar(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza serviço (inclusive ativar/desativar)")
    public ServicoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ServicoRequest request) {
        return ServicoResponse.de(service.atualizar(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove serviço (perfil ADMIN). OS existentes mantêm nome e preço congelados")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
