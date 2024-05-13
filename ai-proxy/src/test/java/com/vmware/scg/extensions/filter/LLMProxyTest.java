package com.vmware.scg.extensions.filter;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "50000")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)

class LLMProxyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LLMProxyTest.class);

    final WireMockServer wireMock = new WireMockServer(9090);

    @Autowired
    WebTestClient webTestClient;

    @BeforeAll
    void setUp() {
        wireMock.stubFor(post("/chat").willReturn(ok()));
        wireMock.start();
    }

    @AfterAll
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void shouldApplyExtensionFilter() {
        String result = webTestClient.post()
                .uri("/chat")
                .header("x-llm", "ollama")
                .bodyValue("Say hi!")
                .exchange()
                .expectStatus()
                .isOk().returnResult(String.class).getResponseBody().map(String::toString).blockLast();
        LOGGER.info(result);
//        wireMock.verify(postRequestedFor(urlPathEqualTo("/chat")));
    }

    @SpringBootApplication
    public static class GatewayApplication {

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
            SpringApplication.run(GatewayApplication.class, args);
        }
    }
}