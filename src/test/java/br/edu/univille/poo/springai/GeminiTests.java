package br.edu.univille.poo.springai;
import br.edu.univille.poo.springai.service.GeminiService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class GeminiTests {
 @Test void promptNaoRecebeSegredo() {
  var service=new GeminiService(ChatClient.builder(mock(ChatModel.class)));
  String prompt=service.montarPrompt("objetos","_A_A",List.of("A","B"));
  assertTrue(prompt.contains("_A_A")); assertTrue(prompt.contains("4")); assertFalse(prompt.contains("CASA"));
 }
 @Test void rejeitaRespostaInvalidaERepetida() {
  ChatClient client=mock(ChatClient.class, RETURNS_DEEP_STUBS);
  ChatClient.Builder builder=mock(ChatClient.Builder.class);
  when(builder.build()).thenReturn(client);
  var service=new GeminiService(builder);
  for(String texto : List.of("", "AB", "A porque sim", "A")) {
   when(client.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(texto);
   assertThrows(IllegalStateException.class,()->service.escolherLetra("tema","_A_A",List.of("A")));
  }
  when(client.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(" r ");
  assertEquals("R",service.escolherLetra("tema","_A_A",List.of("A")));
 }
}
