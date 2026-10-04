package com.pytm.reservationservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Reservation Service API",
                version = "1.0",
                description = "Ticket reservation service with PostgreSQL-backed concurrency control, idempotency and observability"
        )
)
public class OpenApiConfig {
}