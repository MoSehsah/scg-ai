package com.vmware.scg.extensions;

import com.vmware.scg.extensions.filter.LLMProxyGatewayFilterFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayApp {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               LLMProxyGatewayFilterFactory filterFactory) {
        return builder.routes()
                .route("test_route", r -> r.path("/chat/**")
                        .filters(f -> f.filters(filterFactory.apply(new LLMProxyGatewayFilterFactory.Config())))
                        .uri("http://localhost:9090"))
                .build();
    }
    public static void main(String[] args) {
        SpringApplication.run(GatewayApp.class, args);
    }

}