package br.com.viafluvial.acervodigital.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@io.swagger.v3.oas.annotations.OpenAPIDefinition(
    info = @io.swagger.v3.oas.annotations.info.Info(
        title = "api-acervo-digital",
        version = "1.0.0",
        description = "Gerencia ativos de conteudo digital da plataforma, incluindo documentos, imagens e evidencias vinculadas a entidades operacionais."),
    servers = {
        @io.swagger.v3.oas.annotations.servers.Server(url = "/acervo-digital/api/v1", description = "Entrypoint padrao no API Gateway (Gravitee).")
    },
    security = {
        @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    OpenAPI apiInfo() {
        return new OpenAPI()
            .servers(List.of(
                new Server()
                    .url("/acervo-digital/api/v1")
                    .description("Entrypoint padrao no API Gateway (Gravitee).")))
            .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearerAuth"))
            .info(new Info()
                .title("api-acervo-digital")
                .version("1.0.0")
                .description("Gerencia ativos de conteudo digital da plataforma, incluindo documentos, imagens e evidencias vinculadas a entidades operacionais."));
    }
}
