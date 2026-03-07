package com.tlavu.linkforge.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

        @Value("${app.backend.url:http://localhost:8080}")
        private String backendUrl;

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("LinkForge API")
                                                .version("1.0")
                                                .description("REST API Documentation for the LinkForge URL Shortener Project"))
                                .servers(List.of(
                                                new Server().url(backendUrl)
                                                                .description("Current Environment Server")));
        }
}
