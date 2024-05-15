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
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

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

                    return response.writeWith(request.getBody().map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        String body = new String(bytes, StandardCharsets.UTF_8);
                        LOGGER.info("Request body: " + body);
                        Prompt prompt = new Prompt(body);
                        ChatResponse chatResponse = chatClient.call(prompt);
                        return response.bufferFactory().wrap(chatResponse.getResult().getOutput().getContent().getBytes());
                    })



                    );



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
