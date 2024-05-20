package com.vmware.scg.extensions.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptions;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class LLMProxyGatewayFilterFactory extends AbstractGatewayFilterFactory<LLMProxyGatewayFilterFactory.Config> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LLMProxyGatewayFilterFactory.class);

    public LLMProxyGatewayFilterFactory() {
        super(Config.class);
    }

    private static Mono<Void> callChatClient(ServerHttpResponse response, ServerHttpRequest request, ChatClient chatClient, ChatOptions options) {
        return response.writeWith(request.getBody().map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    String body = new String(bytes, StandardCharsets.UTF_8);
                    LOGGER.info("Request body: " + body);
                    Prompt prompt = new Prompt(body, options);
                    ChatResponse chatResponse = chatClient.call(prompt);
                    return response.bufferFactory().wrap(chatResponse.getResult().getOutput().getContent().getBytes());
                })


        );
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            LOGGER.info("Request received" + route.toString());
            LOGGER.info("Headers: " + exchange.getRequest().getHeaders().toString());
            String selectedLLM = config.model;
            Float temperature = config.temperature;
            String[] providers = new String[]{"ollama", "openai"};
            if (selectedLLM == null || selectedLLM.isEmpty()) {
                LOGGER.warn("No LLM selected, passing through...");
            } else if (!Arrays.asList(providers).contains(selectedLLM)) {
                LOGGER.warn("Invalid LLM selected, passing through...");
            } else {
                ChatClient chatClient;
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();
                ServerHttpRequest.Builder builder = exchange.getRequest().mutate();


                if (selectedLLM.equals("ollama")) {
                    LOGGER.info("LLM selected: " + selectedLLM);
                    var ollamaApi = new OllamaApi();
                    chatClient = new OllamaChatClient(ollamaApi);
                    ChatOptions options = new OllamaOptions().withModel(OllamaOptions.DEFAULT_MODEL).withTemperature(temperature != null ? temperature : 0.9f);
                    return callChatClient(response, request,chatClient, options);

                } else if (selectedLLM.equals("openai")) {
                    LOGGER.info("LLM selected: " + selectedLLM);

                    String key = config.APIKey;
                    if (key == null || key.isEmpty()) {
                        LOGGER.error("No API key provided for OpenAI, passing through...");
                    } else {
                        chatClient = new OpenAiChatClient(new OpenAiApi(key));
                        ChatOptions options =  OpenAiChatOptions.builder().withTemperature(temperature != null ? temperature : 0.9f).build();
                        return callChatClient(response, request, chatClient, options);
                    }
                }


                return chain.filter(exchange.mutate().request(builder.build()).build());


            }
            ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
            return chain.filter(exchange.mutate().request(builder.build()).build());

        };
    }

    public static class Config {
        //Put the configuration properties for your filter here
        String model;
        Float temperature;
        String APIKey;

        public Config() {

        }

        public Config(String model, Float temperature, String APIKey) {
            this.model = model;
            this.temperature = temperature;
            this.APIKey = APIKey;
        }


        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Float getTemperature() {
            return temperature;
        }

        public void setTemperature(Float temperature) {
            this.temperature = temperature;
        }

        public String getAPIKey() {
            return APIKey;
        }

        public void setAPIKey(String APIKey) {
            this.APIKey = APIKey;
        }
    }
}
