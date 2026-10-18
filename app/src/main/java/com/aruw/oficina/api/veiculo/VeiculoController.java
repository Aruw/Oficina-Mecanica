package com.aruw.oficina.api.veiculo;

import com.aruw.oficina.api.validation.PlacaValida;
import com.aruw.oficina.application.veiculo.VeiculoCommand;
import com.aruw.oficina.application.veiculo.VeiculoService;
import com.aruw.oficina.domain.veiculo.Veiculo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/veiculos")
@Tag(name = "Veículos", description = "CRUD de veículos (placa padrão antigo ou Mercosul)")
@SecurityRequirement(name = "bearerAuth")
public class VeiculoController {

    private final VeiculoService service;

    public VeiculoController(VeiculoService service) {
        this.service = service;
    }

    public record VeiculoRequest(
            @NotBlank @PlacaValida String placa,
            @NotBlank @Size(max = 60) String marca,
            @NotBlank @Size(max = 80) String modelo,
            @NotNull @Min(1900) @Max(2100) Integer ano,
            @NotNull UUID clienteId) {

        VeiculoCommand toCommand() {
            return new VeiculoCommand(placa, marca, modelo, ano, clienteId);
        }
    }

    public record VeiculoResponse(UUID id, String placa, boolean mercosul, String marca, String modelo, int ano,
                                  UUID clienteId) {
        static VeiculoResponse de(Veiculo v) {
            return new VeiculoResponse(v.getId(), v.getPlaca().getValor(), v.getPlaca().isMercosul(), v.getMarca(),
                    v.getModelo(), v.getAno(), v.getClienteId());
        }
    }

    @PostMapping
    @Operation(summary = "Cadastra veículo vinculado a um cliente")
    public ResponseEntity<VeiculoResponse> criar(@Valid @RequestBody VeiculoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(VeiculoResponse.de(service.criar(request.toCommand())));
    }

    @GetMapping
    @Operation(summary = "Lista veículos (opcionalmente de um cliente)")
    public List<VeiculoResponse> listar(@RequestParam(required = false) UUID clienteId) {
        return service.listar(clienteId).stream().map(VeiculoResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha veículo")
    public VeiculoResponse buscar(@PathVariable UUID id) {
        return VeiculoResponse.de(service.buscar(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza marca, modelo, ano e proprietário (a placa é imutável)")
    public VeiculoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody VeiculoRequest request) {
        return VeiculoResponse.de(service.atualizar(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove veículo sem OS vinculadas (perfil ADMIN)")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
