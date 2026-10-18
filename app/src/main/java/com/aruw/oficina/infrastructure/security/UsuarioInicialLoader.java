package com.aruw.oficina.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UsuarioInicialLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuarioInicialLoader.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public UsuarioInicialLoader(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                                @Value("${app.security.admin.username:}") String username,
                                @Value("${app.security.admin.password:}") String password) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank()) {
            log.warn("ADMIN_USERNAME/ADMIN_PASSWORD não definidos: nenhum usuário inicial foi criado");
            return;
        }
        if (!usuarios.existsByUsername(username)) {
            usuarios.save(new Usuario(username, passwordEncoder.encode(password), Usuario.Perfil.ADMIN));
            log.info("Usuário administrador inicial '{}' criado", username);
        }
    }
}
