package com.inventory.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory & Order Management API")
                        .description("""
                                Secure REST API for managing products, categories, inventory, and orders.
                                
                                **Roles:**
                                - `ROLE_ADMIN` — manage categories, products, inventory, view all orders
                                - `ROLE_USER`  — browse products, place and manage own orders
                                
                                Register with role `ROLE_ADMIN` or `ROLE_USER`, login to get a JWT token, 
                                then click **Authorize** and paste the token.
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("Inventory API").email("admin@inventory.com")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components()
                        .addSecuritySchemes(SCHEME, new SecurityScheme()
                                .name(SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
