package br.edu.univille.poo.springai;
import br.edu.univille.poo.springai.service.*;
import br.edu.univille.poo.springai.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:testes", "spring.config.import=", "spring.ai.model.chat=none"})
@AutoConfigureMockMvc
class ForcaTests {
 @Autowired ForcaService service;
 @Autowired MockMvc mvc;
 @MockitoBean GeminiService gemini;
 @Test void revelaTodasOcorrenciasEGanha() {
  var p=service.iniciar("\u00e1rara", "animais");
  when(gemini.escolherLetra(anyString(),anyString(),anyList())).thenReturn("A","R");
  assertEquals("A_A_A",service.jogar(p.getId()).mascara());
  var fim=service.jogar(p.getId());
  assertEquals(ResultadoPartida.VITORIA_IA,fim.resultado()); assertEquals("ARARA",fim.palavra());
  assertThrows(IllegalArgumentException.class,()->service.jogar(p.getId()));
 }
 @Test void perdeExatamenteNoQuintoErro() {
  var p=service.iniciar("casa","objetos");
  when(gemini.escolherLetra(anyString(),anyString(),anyList())).thenReturn("B","D","F","G","H");
  for(int i=1;i<=5;i++) { var e=service.jogar(p.getId()); assertEquals(i,e.erros()); assertEquals(i==5,e.finalizada()); }
  assertEquals(ResultadoPartida.DERROTA_IA,service.estado(p.getId()).resultado());
 }
 @Test void falhaNaoConsomeJogada() {
  var p=service.iniciar("casa","objetos");
  when(gemini.escolherLetra(anyString(),anyString(),anyList())).thenThrow(new IllegalStateException("Falha"));
  assertThrows(IllegalStateException.class,()->service.jogar(p.getId()));
  var e=service.estado(p.getId()); assertEquals(0,e.erros()); assertTrue(e.letras().isEmpty()); assertNull(e.palavra());
 }
 @Test void validaEntrada() {
  for(String palavra : new String[]{"a","duas palavras","abc1","abc!"}) assertThrows(IllegalArgumentException.class,()->service.iniciar(palavra,"tema"));
  assertThrows(IllegalArgumentException.class,()->service.iniciar("casa"," "));
  assertEquals("ACAO",ForcaService.normalizar("a\u00e7\u00e3o"));
 }
 @Test void telasERotas() throws Exception {
  mvc.perform(get("/")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/forca")).andExpect(status().isOk());
  var p=service.iniciar("casa","objetos");
  mvc.perform(get("/forca/jogar/"+p.getId())).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("CASA"))));
  mvc.perform(get("/forca/jogar/999999")).andExpect(status().isNotFound());
  when(gemini.escolherLetra(anyString(),anyString(),anyList())).thenThrow(new IllegalStateException("Falha"));
  mvc.perform(post("/forca/jogar/"+p.getId()+"/genai")).andExpect(status().isBadGateway());
 }
}
