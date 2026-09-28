package es.upm.miw.oneleft.users.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI del servicio. El servidor es el API Gateway, de modo que las pruebas desde
 * Swagger UI siguen el mismo camino que la app, y la seguridad es Keycloak (Authorization Code + PKCE).
 */
@Configuration
public class OpenApiConfig {

    public static final String KEYCLOAK = "keycloak";

    @Bean
    OpenAPI openApi(@Value("${oneleft.openapi.gateway-url:http://localhost:8080}") String gatewayUrl,
                    @Value("${KEYCLOAK_ISSUER:http://localhost:8180/realms/oneleft}") String issuer) {
        var flow = new OAuthFlow()
                .authorizationUrl(issuer + "/protocol/openid-connect/auth")
                .tokenUrl(issuer + "/protocol/openid-connect/token")
                .scopes(new Scopes().addString("openid", "Identidad").addString("profile", "Nombre")
                        .addString("email", "Email"));
        return new OpenAPI()
                .info(new Info().title("OneLeft · users").version("v1").description("Usuarios, perfiles y reputación de OneLeft."))
                .addServersItem(new Server().url(gatewayUrl).description("API Gateway"))
                .components(new Components().addSecuritySchemes(KEYCLOAK, new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .description("Inicio de sesión con Keycloak")
                        .flows(new OAuthFlows().authorizationCode(flow))))
                .addSecurityItem(new SecurityRequirement().addList(KEYCLOAK));
    }
}
