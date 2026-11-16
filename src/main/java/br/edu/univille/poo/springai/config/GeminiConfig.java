package br.edu.univille.poo.springai.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {
    @Bean(destroyMethod = "close")
    public Client geminiClient(@Value("${spring.ai.google.genai.api-key}") String chave) {
        // As novas tentativas são controladas pelo serviço para aparecerem na interface.
        return Client.builder().apiKey(chave).httpOptions(HttpOptions.builder()
                .timeout(20_000)
                .retryOptions(HttpRetryOptions.builder().attempts(1).build())
                .build()).build();
    }
}
