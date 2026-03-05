package com.tlavu.linkforge.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "LinkForge API", version = "1.0", description = "REST API Documentation for the LinkForge URL Shortener Project", contact = @Contact(name = "Support", email = "truongleanhvu20052004@gmail.com"), license = @License(name = "MIT License", url = "https://opensource.org/licenses/MIT")), servers = {
                @Server(description = "Local DEV Environment", url = "http://localhost:8080")
})
public class OpenApiConfig {
}
