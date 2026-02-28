package com.e2d.gateway.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class GatewayRoutes {

        private final JwtAuthFilter jwtFilter;

        public RouteLocator routes(RouteLocatorBuilder builder) {
                return builder.routes()
                                .route("auth-service", r -> r.path("/auth/**")
                                                .uri("http://localhost:8081"))
                                .route("ticket-service", r -> r.path("/api/ticket/**")
                                                .filters(f -> f.filter(jwtFilter.apply(new JwtAuthFilter.Config())))
                                                .uri("http://localhost:8088"))
                                .build();
        }
}
