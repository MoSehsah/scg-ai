package com.vmware.scg.extensions.filter;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class LLMProxyGatewayFilterFactory extends AbstractGatewayFilterFactory<LLMProxyGatewayFilterFactory.Config> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LLMProxyGatewayFilterFactory.class);

    public LLMProxyGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            LOGGER.info("Request received" + route.toString());
            LOGGER.info("Headers: " + exchange.getRequest().getHeaders().toString());
            String selectedLLM = exchange.getRequest().getHeaders().getFirst("x-llm");
            if (selectedLLM == null) {
                LOGGER.info("No LLM header found");
            } else {
                if (selectedLLM.equals("ollama")) {
                    LOGGER.info("LLM header found: " + selectedLLM);

                    ServerHttpRequest request = exchange.getRequest();
                    ServerHttpResponse response = exchange.getResponse();

                    var ollamaApi = new OllamaApi();
                    var chatClient = new OllamaChatClient(ollamaApi).withDefaultOptions(OllamaOptions.create().withModel(OllamaOptions.DEFAULT_MODEL).withTemperature(0.9f));
                    Flux<ChatResponse> chatResponse = chatClient.stream(new Prompt(request.getBody().toString()));
//                    chatResponse.map(val -> val.getResult().getOutput().getContent()).subscribe(System.out::print);
                    Flux<String> chatResponseString = chatResponse.map(val -> val.getResult().getOutput().getContent());

                    response.getHeaders().set("Content-Type", "application/text");
                    DataBufferFactory bufferFactory = response.bufferFactory();
                    Publisher<DataBuffer> dataBufferPublisher = chatResponseString.map(str -> bufferFactory.wrap(str.getBytes()));
                    return response.writeWith(dataBufferPublisher);


//                    String responseString = "{\"response\": \"what is your model version?\"}";
//                    response.setStatusCode(HttpStatus.OK);
//                    response.getHeaders().add("Content-Type", "application/json");
//                    return response.writeWith(Mono.just(responseString).map(str -> response.bufferFactory().wrap(str.getBytes())));


                } else if (selectedLLM.equals("gpt3")) {
                    LOGGER.info("LLM header found: " + selectedLLM);


                }
            }
            ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
            return chain.filter(exchange.mutate().request(builder.build()).build());
        };
    }

    public static class Config {
        //Put the configuration properties for your filter here
    }
}
