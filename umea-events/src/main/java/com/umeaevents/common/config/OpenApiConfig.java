package com.umeaevents.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sätter titel/version/beskrivning som visas överst i Swagger UI.
 * Inte nödvändigt för att API:t ska fungera, men gör dokumentationen
 * proffsig från start.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI umeaEventsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Umeå Events API")
                .version("v1")
                .description("API för lokala restaurang- och nöjesevent i Umeå"));
    }
}
