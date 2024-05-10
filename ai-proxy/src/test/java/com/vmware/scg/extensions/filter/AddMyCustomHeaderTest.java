package com.vmware.scg.extensions.filter;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddMyCustomHeaderTest {

    final WireMockServer wireMock = new WireMockServer(9090);

    @Autowired
    WebTestClient webTestClient;

    @BeforeAll
    void setUp() {
        wireMock.stubFor(get("/add-header").willReturn(ok()));
        wireMock.start();
    }

    @AfterAll
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void shouldApplyExtensionFilter() {
        webTestClient.get()
                .uri("/add-header")
                .exchange()
                .expectStatus()
                .isOk();

        wireMock.verify(getRequestedFor(urlPathEqualTo("/add-header"))
                .withHeader("X-My-Header", new EqualToPattern("my-header-value")));
    }

    @SpringBootApplication
    public static class GatewayApplication {

        @Bean
        public RouteLocator routes(RouteLocatorBuilder builder,
                                   AddMyCustomHeaderGatewayFilterFactory filterFactory) {
            return builder.routes()
                    .route("test_route", r -> r.path("/add-header/**")
                            .filters(f -> f.filters(filterFactory.apply(new Object())))
                            .uri("http://localhost:9090"))
                    .build();
        }

        public static void main(String[] args) {
            SpringApplication.run(GatewayApplication.class, args);
        }
    }
}