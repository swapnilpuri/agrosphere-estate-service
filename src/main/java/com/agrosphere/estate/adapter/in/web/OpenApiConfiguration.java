package com.agrosphere.estate.adapter.in.web;
import org.springframework.context.annotation.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
@Configuration
public class OpenApiConfiguration {
    @Bean OpenAPI estateApi() { return new OpenAPI().info(new Info().title("AgroSphere Estate Service").version("0.2.0"))
        .components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))); }
}
