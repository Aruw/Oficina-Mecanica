package com.aruw.oficina.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Oficina Mecânica — API",
        version = "1.0.0",
        description = """
                MVP do Sistema Integrado de Atendimento e Execução de Serviços.

                1. Obtenha um token em **POST /api/auth/login**;
                2. Clique em **Authorize** e informe o token;
                3. As rotas **/api/public/** são usadas pelo cliente para acompanhar e aprovar a OS (sem token, com CPF/CNPJ).
                """))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

}