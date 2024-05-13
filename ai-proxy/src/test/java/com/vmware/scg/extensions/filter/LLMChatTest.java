package com.vmware.scg.extensions.filter;

import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class LLMChatTest {

    public static void main(String[] args) {
        SpringApplication.run(LLMChatTest.class, args);
    }
    @Bean
    public CommandLineRunner demo() {
        return args -> {
            var ollamaApi = new OllamaApi();
            var chatClient = new OllamaChatClient(ollamaApi).withDefaultOptions(OllamaOptions.create().withModel(OllamaOptions.DEFAULT_MODEL).withTemperature(0.9f));
            Flux<ChatResponse> chatResponse = chatClient.stream(new Prompt("Say hi!"));
            chatResponse.map(val -> val.getResult().getOutput().getContent()).subscribe(System.out::print);
        };
    }

}
