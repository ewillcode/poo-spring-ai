package br.edu.univille.poo.springai;

import br.edu.univille.poo.springai.service.GeminiTentativas;
import com.google.genai.errors.ApiException;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeminiTentativasTests {
    private GeminiTentativas comResposta(String corpo, String retryAfter) throws IOException {
        var politica = new GeminiTentativas(0);
        var request = new Request.Builder().url("https://example.com").build();
        var chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(request)).thenReturn(new Response.Builder().request(request)
                .protocol(Protocol.HTTP_1_1).code(429).message("Quota")
                .header("Retry-After", retryAfter)
                .body(ResponseBody.create(corpo, MediaType.get("application/json"))).build());
        try (var resposta = politica.observar(chain)) { assertEquals(429, resposta.code()); }
        return politica;
    }

    @Test void naoRepeteErrosPermanentes() {
        var politica = new GeminiTentativas(0);
        for (int codigo : new int[]{400, 401, 403, 404}) {
            var falha = politica.classificar(new ApiException(codigo, "ERRO", "Detalhe privado"));
            assertFalse(falha.repetir());
            assertTrue(falha.mensagem().contains(String.valueOf(codigo)));
            assertFalse(falha.mensagem().contains("privado"));
        }
    }

    @Test void distingueCotaDiariaECotaZero() throws Exception {
        for (String detalhe : new String[]{"{\"error\":{\"message\":\"RequestsPerDay\"}}",
                "{\"error\":{\"details\":[{\"quotaValue\":\"0\"}]}}"}) {
            assertFalse(comResposta(detalhe, "1").classificar(new RuntimeException()).repetir());
        }
    }

    @Test void respeitaRetryInfoERetryAfter() throws Exception {
        var falha = comResposta("{\"error\":{\"details\":[{\"retryDelay\":\"45s\"}]}}", "1")
                .classificar(new RuntimeException());
        assertTrue(falha.repetir());
        assertEquals(60_000, falha.esperaMinimaMs());
        var longa = comResposta("{\"error\":{\"details\":[{\"retryDelay\":\"120s\"}]}}", "90")
                .classificar(new RuntimeException());
        assertFalse(longa.repetir());
        assertEquals(120_000, longa.esperaMinimaMs());
        assertEquals(90_000, comResposta("{}", "90").classificar(new RuntimeException()).esperaMinimaMs());
    }

    @Test void backoffCresceERepeteSomenteFalhasTransitorias() {
        var politica = new GeminiTentativas(0);
        var falha = politica.classificar(new ApiException(503, "UNAVAILABLE", "Erro"));
        assertTrue(falha.repetir());
        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            long atraso = politica.atraso(falha, tentativa);
            long base = 5000L * (1L << (tentativa - 1));
            assertTrue(atraso >= base && atraso <= base + 1000);
        }
        assertTrue(politica.classificar(new RuntimeException(new IOException())).repetir());
        assertFalse(politica.classificar(new RuntimeException()).repetir());
    }
}
