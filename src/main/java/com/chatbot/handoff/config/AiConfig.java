package com.chatbot.handoff.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;

@Configuration
public class AiConfig {

    @Value("${spring.ai.google.genai.embedding.api-key}")
    private String apiKey;

    @Bean
    public GoogleGenAiEmbeddingConnectionDetails googleGenAiEmbeddingConnectionDetails() {
        Client genAiClient = Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .apiVersion("v1beta")
                        .build())
                .build();

        return GoogleGenAiEmbeddingConnectionDetails.builder()
                .apiKey(apiKey)
                .projectId("handoff-project")
                .genAiClient(genAiClient)
                .build();
    }

    @Bean
    public GoogleGenAiTextEmbeddingModel googleGenAiTextEmbeddingModel(
            GoogleGenAiEmbeddingConnectionDetails connectionDetails) {
        GoogleGenAiTextEmbeddingOptions options = GoogleGenAiTextEmbeddingOptions.builder()
                .model("gemini-embedding-2")
                .build();

        return new GoogleGenAiTextEmbeddingModel(connectionDetails, options);
    }

    @Bean
    public SimpleVectorStore vectorStore(GoogleGenAiTextEmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
