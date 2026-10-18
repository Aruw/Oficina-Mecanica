package com.aruw.oficina.api.cliente;

import com.aruw.oficina.api.validation.CpfOuCnpj;
import com.aruw.oficina.application.cliente.ClienteCommand;
import com.aruw.oficina.application.cliente.ClienteService;
import com.aruw.oficina.domain.cliente.Cliente;
import com.aruw.oficina.domain.shared.Documento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/clientes")
@Tag(name = "Clientes", description = "CRUD de clientes (identificados por CPF/CNPJ)")
@SecurityRequirement(name = "bearerAuth")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    public record ClienteRequest(
            @NotBlank @Size(max = 150) String nome,
            @NotBlank @CpfOuCnpj String documento,
            @Email @Size(max = 150) String email,
            @Size(max = 20) String telefone) {

        ClienteCommand toCommand() {
            return new ClienteCommand(nome, documento, email, telefone);
        }
    }

    public record ClienteResponse(UUID id, String nome, String documento, Documento.Tipo tipoDocumento, String email,
                                  String telefone, Instant criadoEm) {
        static ClienteResponse de(Cliente c) {
            return new ClienteResponse(c.getId(), c.getNome(), c.getDocumento().formatado(), c.getDocumento().getTipo(),
                    c.getEmail(), c.getTelefone(), c.getCriadoEm());
        }
    }

    @PostMapping
    @Operation(summary = "Cadastra cliente")
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ClienteResponse.de(service.criar(request.toCommand())));
    }

    @GetMapping
    @Operation(summary = "Lista clientes ou busca por documento (CPF/CNPJ)")
    public List<ClienteResponse> listar(@RequestParam(required = false) String documento) {
        if (documento != null && !documento.isBlank()) {
            return List.of(ClienteResponse.de(service.buscarPorDocumento(documento)));
        }
        return service.listar().stream().map(ClienteResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha cliente")
    public ClienteResponse buscar(@PathVariable UUID id) {
        return ClienteResponse.de(service.buscar(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome e contatos do cliente (o documento é imutável)")
    public ClienteResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ClienteRequest request) {
        return ClienteResponse.de(service.atualizar(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove cliente sem veículos/OS vinculados (perfil ADMIN)")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
