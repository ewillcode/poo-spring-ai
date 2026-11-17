package br.edu.univille.poo.springai;
import br.edu.univille.poo.springai.service.GeminiService;
import br.edu.univille.poo.springai.service.GeminiTentativas;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class GeminiTests {
 private GeminiTentativas semEspera() {
  var tentativas=spy(new GeminiTentativas(0));
  doNothing().when(tentativas).esperar(anyLong(),any());
  return tentativas;
 }
 @Test void naoInsisteEmModeloIndisponivel() {
  ChatClient client=mock(ChatClient.class, RETURNS_DEEP_STUBS);
  ChatClient.Builder builder=mock(ChatClient.Builder.class);
  when(builder.build()).thenReturn(client);
  var chamada=client.prompt().system(anyString()).user(anyString()).call();
  when(chamada.content()).thenThrow(new com.google.genai.errors.ApiException(404,"NOT_FOUND","Modelo indisponível"));
  assertThrows(IllegalStateException.class,()->new GeminiService(builder,semEspera()).escolherLetra("tema","__",List.of()));
  verify(chamada,times(1)).content();
 }

 @Test void promptNaoRecebeSegredo() {
  var service=new GeminiService(ChatClient.builder(mock(ChatModel.class)), semEspera());
  String prompt=service.montarPrompt("objetos","_A_A",List.of("A","B"));
  assertTrue(prompt.contains("_A_A")); assertTrue(prompt.contains("4")); assertFalse(prompt.contains("CASA"));
 }
 @Test void repeteAteTresVezesEInformaProgresso() {
  ChatClient client=mock(ChatClient.class, RETURNS_DEEP_STUBS);
  ChatClient.Builder builder=mock(ChatClient.Builder.class);
  when(builder.build()).thenReturn(client);
  var chamada=client.prompt().system(anyString()).user(anyString()).call();
  when(chamada.content()).thenThrow(new RuntimeException(new java.io.IOException("falha")))
    .thenThrow(new RuntimeException(new java.io.IOException("falha"))).thenThrow(new RuntimeException(new java.io.IOException("falha"))).thenReturn("B");
  var mensagens=new ArrayList<String>();
  assertEquals("B",new GeminiService(builder, semEspera()).escolherLetra("tema","_A_A",List.of("A"),mensagens::add));
  verify(chamada,times(4)).content();
  assertTrue(mensagens.stream().anyMatch(m -> m.contains("tentativa 4/4")));
  assertTrue(mensagens.get(mensagens.size()-1).contains("letra B"));
 }
 @Test void encerraDepoisDaQuartaFalha() {
  ChatClient client=mock(ChatClient.class, RETURNS_DEEP_STUBS);
  ChatClient.Builder builder=mock(ChatClient.Builder.class);
  when(builder.build()).thenReturn(client);
  var chamada=client.prompt().system(anyString()).user(anyString()).call();
  when(chamada.content()).thenThrow(new RuntimeException(new java.io.IOException("falha")));
  assertThrows(IllegalStateException.class,()->new GeminiService(builder, semEspera()).escolherLetra("tema","__",List.of()));
  verify(chamada,times(4)).content();
 }
 @Test void rejeitaRespostaInvalidaERepetida() {
  ChatClient client=mock(ChatClient.class, RETURNS_DEEP_STUBS);
  ChatClient.Builder builder=mock(ChatClient.Builder.class);
  when(builder.build()).thenReturn(client);
  var service=new GeminiService(builder, semEspera());
  for(String texto : List.of("", "AB", "A porque sim", "A")) {
   when(client.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(texto);
   assertThrows(IllegalStateException.class,()->service.escolherLetra("tema","_A_A",List.of("A")));
  }
  when(client.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(" r ");
  assertEquals("R",service.escolherLetra("tema","_A_A",List.of("A")));
 }
}
