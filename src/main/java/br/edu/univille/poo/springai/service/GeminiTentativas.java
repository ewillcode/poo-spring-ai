package br.edu.univille.poo.springai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.errors.ApiException;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Component
public class GeminiTentativas {
    public record Falha(boolean repetir, String mensagem, long esperaMinimaMs) {}
    private record Resposta(int codigo, String corpo, String retryAfter) {}
    private final ThreadLocal<Resposta> ultimaResposta = new ThreadLocal<>();
    private final long intervaloMs;
    private long proximaChamada;

    public GeminiTentativas(@Value("${forca.gemini.intervalo-ms:6500}") long intervaloMs) {
        this.intervaloMs = Math.max(0, intervaloMs);
    }

    // Preserva somente os detalhes necessários para classificar a falha; não registra a chave.
    public Response observar(Interceptor.Chain chamada) throws IOException {
        Response resposta = chamada.proceed(chamada.request());
        if (!resposta.isSuccessful()) ultimaResposta.set(new Resposta(resposta.code(),
                resposta.peekBody(65_536).string(), resposta.header("Retry-After")));
        return resposta;
    }

    public void limpar() { ultimaResposta.remove(); }

    public void aguardarIntervalo(Consumer<String> progresso) {
        long espera;
        synchronized (this) {
            long agora = System.currentTimeMillis();
            espera = Math.max(0, proximaChamada - agora);
            proximaChamada = Math.max(agora, proximaChamada) + intervaloMs;
        }
        esperar(espera, progresso);
    }

    public void esperar(long milissegundos, Consumer<String> progresso) {
        long restante = milissegundos;
        while (restante > 0) {
            progresso.accept("Aguardando " + (long)Math.ceil(restante / 1000.0)
                    + " s antes da próxima chamada ao Gemini...");
            long pausa = Math.min(restante, 1000);
            try { Thread.sleep(pausa); }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("A espera pela IA foi interrompida.");
            }
            restante -= pausa;
        }
    }

    public long atraso(Falha falha, int tentativa) {
        return Math.max(falha.esperaMinimaMs(), 5000L * (1L << (tentativa - 1)))
                + ThreadLocalRandom.current().nextLong(1001);
    }

    public Falha classificar(Throwable erro) {
        Resposta resposta = ultimaResposta.get();
        int codigo = resposta == null ? 0 : resposta.codigo();
        String detalhes = resposta == null ? "" : resposta.corpo();
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof ApiException api) {
                codigo = api.code();
                if (detalhes.isBlank()) detalhes = api.message();
                break;
            }
        }
        if (codigo == 400 || codigo == 401 || codigo == 403)
            return new Falha(false, "Gemini HTTP " + codigo + ": confira a chave do AI Studio, as restrições da chave e o acesso do projeto à API.", 0);
        if (codigo == 404)
            return new Falha(false, "Gemini HTTP 404: modelo indisponível para esta chave. Confira GEMINI_MODEL no .env.", 0);
        if (codigo == 429) {
            String texto = detalhes.toLowerCase(Locale.ROOT);
            boolean diaria = texto.contains("perday") || texto.contains("per_day") || texto.contains("daily");
            boolean semCota = Pattern.compile("(?:quotaValue|quota_value)\"?\\s*:\\s*\"?0(?:\"|\\s|,|})", Pattern.CASE_INSENSITIVE).matcher(detalhes).find()
                    || texto.contains("limit: 0");
            if (diaria || semCota) return new Falha(false,
                    "Gemini HTTP 429: cota diária esgotada ou cota gratuita indisponível. Consulte os limites do projeto no AI Studio; repetir agora não resolve.", 0);
            long espera = 60_000;
            if (resposta != null) espera = Math.max(espera, retryAfterMs(resposta.retryAfter()));
            try {
                JsonNode itens = new ObjectMapper().readTree(detalhes).path("error").path("details");
                for (JsonNode item : itens) {
                    String valor = item.path("retryDelay").asText("");
                    if (valor.endsWith("s")) espera = Math.max(espera,
                            (long)Math.ceil(Double.parseDouble(valor.substring(0, valor.length()-1)) * 1000));
                }
            } catch (Exception ignorado) { /* Usa espera conservadora quando não houver detalhes válidos. */ }
            if (espera > 60_000) return new Falha(false,
                    "Gemini HTTP 429: o servidor pediu uma espera de " + (long)Math.ceil(espera/1000.0)
                            + " s. Aguarde esse prazo antes de solicitar outro palpite.", espera);
            return new Falha(true, "Gemini HTTP 429: limite temporário de requisições. Aguardaremos pelo menos 60 segundos.", espera);
        }
        if (codigo == 408 || codigo == 500 || codigo == 502 || codigo == 503 || codigo == 504) {
            long espera = resposta == null ? 0 : retryAfterMs(resposta.retryAfter());
            if (espera > 60_000) return new Falha(false, "Gemini HTTP " + codigo
                    + ": aguarde " + (long)Math.ceil(espera/1000.0) + " s, conforme orientação do servidor.", espera);
            return new Falha(true, "Gemini HTTP " + codigo + ": falha temporária do serviço.", espera);
        }
        if (codigo != 0) return new Falha(false, "Gemini HTTP " + codigo + ": a solicitação foi recusada; confira a configuração.", 0);
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof IOException)
                return new Falha(true, "Falha de conexão ou timeout ao consultar o Gemini.", 0);
        }
        if (erro instanceof IllegalStateException && erro.getMessage() != null
                && erro.getMessage().startsWith("A IA "))
            return new Falha(true, erro.getMessage(), 0);
        return new Falha(false, "Não foi possível consultar o Gemini. Confira a configuração da aplicação.", 0);
    }

    private long retryAfterMs(String valor) {
        if (valor == null) return 0;
        try { return (long)Math.ceil(Double.parseDouble(valor) * 1000); }
        catch (NumberFormatException e) {
            try { return Math.max(0, ZonedDateTime.parse(valor, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toInstant().toEpochMilli() - System.currentTimeMillis()); }
            catch (Exception ignorado) { return 0; }
        }
    }
}
