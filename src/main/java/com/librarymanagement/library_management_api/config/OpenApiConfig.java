package com.librarymanagement.library_management_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI libraryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Library Management API")
                        .version("v1")
                        .description("""
                                RESTful API for managing a simple library system.
                                Supports registering borrowers and books, listing books,
                                and borrowing/returning book copies.
                                """)
                        .contact(new Contact().name("Your Name"))
                        .license(new License().name("MIT")));
    }
}