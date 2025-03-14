package com.swiftchat.auth_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    /**
     * Creates OpenAPI configuration for Swagger documentation.
     *
     * @return Configured OpenAPI bean
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme()));
    }

    /**
     * Configures API info for OpenAPI documentation.
     *
     * @return Info object with API details
     */
    private Info apiInfo() {
        return new Info()
                .title("SwiftChat Auth Service API")
                .description("API documentation for the SwiftChat Authentication Service")
                .version("1.0.0")
                .contact(new Contact()
                        .name("SwiftChat Team")
                        .email("support@swiftchat.com")
                        .url("https://swiftchat.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    /**
     * Configures security scheme for JWT tokens.
     *
     * @return SecurityScheme for JWT Bearer authentication
     */
    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(
                        "JWT Authorization header using the Bearer scheme. \n\n" +
                                "Enter 'Bearer' [space] and then your token in the text input below.\n\n" +
                                "Example: \"Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"");
    }
}
