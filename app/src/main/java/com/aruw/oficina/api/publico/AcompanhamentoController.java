package com.aruw.oficina.api.publico;

import com.aruw.oficina.api.validation.CpfOuCnpj;
import com.aruw.oficina.application.ordemservico.OrdemServicoDetalhe;
import com.aruw.oficina.application.ordemservico.OrdemServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/public/ordens-servico")
@Tag(name = "Cliente — acompanhamento", description = "Consulta de andamento e aprovação de orçamento pelo cliente")
public class AcompanhamentoController {

    private final OrdemServicoService service;

    public AcompanhamentoController(OrdemServicoService service) {
        this.service = service;
    }

    public record DecisaoRequest(@NotBlank @CpfOuCnpj String documento, @Size(max = 500) String motivo) {
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta o andamento da OS informando o CPF/CNPJ do titular")
    public OrdemServicoDetalhe consultar(@PathVariable UUID id, @RequestParam @NotBlank @CpfOuCnpj String documento) {
        return service.consultarPeloCliente(id, documento);
    }

    @PostMapping("/{id}/orcamento/aprovacao")
    @Operation(summary = "Cliente aprova o orçamento")
    public OrdemServicoDetalhe aprovar(@PathVariable UUID id, @Valid @RequestBody DecisaoRequest request) {
        return service.aprovarPeloCliente(id, request.documento());
    }

    @PostMapping("/{id}/orcamento/rejeicao")
    @Operation(summary = "Cliente rejeita o orçamento")
    public OrdemServicoDetalhe rejeitar(@PathVariable UUID id, @Valid @RequestBody DecisaoRequest request) {
        return service.rejeitarPeloCliente(id, request.documento(), request.motivo());
    }
}
