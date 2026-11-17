package br.edu.univille.poo.springai.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import br.edu.univille.poo.springai.service.GeminiTentativas;
import com.google.genai.types.ClientOptions;
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

@Configuration
public class GeminiConfig {
    @Bean(destroyMethod = "close")
    public Client geminiClient(@Value("${spring.ai.google.genai.api-key}") String chave, GeminiTentativas tentativas) {
        // As novas tentativas são controladas pelo serviço para aparecerem na interface.
        OkHttpClient http = new OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false).addInterceptor(tentativas::observar).build();
        return Client.builder().apiKey(chave)
                .clientOptions(ClientOptions.builder().customHttpClient(http).build()).httpOptions(HttpOptions.builder()
                .timeout(20_000)
                .retryOptions(HttpRetryOptions.builder().attempts(1).build())
                .build()).build();
    }
}
