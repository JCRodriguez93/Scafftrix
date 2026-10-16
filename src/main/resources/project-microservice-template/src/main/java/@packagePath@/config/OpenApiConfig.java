package @packageName@.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos que springdoc publica en {@code /v3/api-docs}.
 *
 * <p>El contrato de origen es {@code src/main/resources/contract/api.yaml}; esta clase solo
 * enriquece la documentacion servida en runtime.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI microserviceOpenApi(@Value("${spring.application.name}") String applicationName) {
        return new OpenAPI().info(new Info()
                .title(applicationName)
                .version("v1")
                .description("API definida por el contrato OpenAPI del microservicio"));
    }
}
