package com.ai.assistant.config;

import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.inmemory.InMemoryVectorStore;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Bean
    public EmbeddingClient embeddingClient() {
        return new OpenAiEmbeddingModel(apiKey);
    }

    @Bean
    public VectorStore vectorStore(EmbeddingClient embeddingClient) {
        return new InMemoryVectorStore(embeddingClient);
    }
}