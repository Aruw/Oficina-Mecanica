package com.aruw.oficina.infrastructure.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                               JwtProperties properties) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public record Token(String accessToken, String tokenType, long expiresIn) {
    }

    @Transactional(readOnly = true)
    public Token autenticar(String username, String senha) {
        Usuario usuario = usuarios.findByUsername(username)
                .filter(Usuario::isAtivo)
                .filter(u -> passwordEncoder.matches(senha, u.getSenhaHash()))
                .orElseThrow(() -> new BadCredentialsException("Usuário ou senha inválidos"));

        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(usuario.getUsername())
                .issuedAt(agora)
                .expiresAt(agora.plus(properties.expiration()))
                .claim("roles", List.of(usuario.getPerfil().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Token(token, "Bearer", properties.expiration().toSeconds());
    }
}
