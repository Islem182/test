package com.dgi.platform.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "DGI Online Services Platform API",
                version = "1.0.0",
                description = "Production-ready Enterprise REST API for DGI Taxpayers, Agents, and Administrators.",
                contact = @Contact(name = "Direction Générale des Impôts", email = "support@dgi.tn")
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter JWT Bearer Token to authorize REST API requests"
)
public class OpenApiConfig {
}
