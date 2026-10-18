package com.aruw.oficina.api.auth;

import com.aruw.oficina.infrastructure.security.AutenticacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Emissão de token JWT para as APIs administrativas")
public class AuthController {

    private final AutenticacaoService autenticacao;

    public AuthController(AutenticacaoService autenticacao) {
        this.autenticacao = autenticacao;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica um usuário interno e retorna o token JWT (Bearer)")
    public AutenticacaoService.Token login(@Valid @RequestBody LoginRequest request) {
        return autenticacao.autenticar(request.username(), request.password());
    }
}
