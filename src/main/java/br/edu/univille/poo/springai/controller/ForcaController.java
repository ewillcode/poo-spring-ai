package br.edu.univille.poo.springai.controller;
import br.edu.univille.poo.springai.service.ForcaService;
import br.edu.univille.poo.springai.dto.EstadoPartidaDTO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.OptimisticLockingFailureException;
import java.util.Map;
@Controller
public class ForcaController {
    private final ForcaService service;
    public ForcaController(ForcaService service) { this.service = service; }
    @GetMapping("/") public String home() { return "redirect:/forca"; }
    @GetMapping("/forca") public ModelAndView index() {
        return new ModelAndView("forca/index", "partidas", service.listar());
    }
    @PostMapping("/forca/iniciar") public ModelAndView iniciar(@RequestParam String palavra, @RequestParam String tema) {
        try { return new ModelAndView("redirect:/forca/jogar/" + service.iniciar(palavra, tema).getId()); }
        catch (IllegalArgumentException e) { ModelAndView mv = index(); mv.addObject("erro", e.getMessage()); mv.setStatus(org.springframework.http.HttpStatus.BAD_REQUEST); return mv; }
    }
    @GetMapping("/forca/jogar/{id}") public ModelAndView tela(@PathVariable Long id) {
        return new ModelAndView("forca/jogar", "estado", service.estado(id));
    }
    @PostMapping("/forca/jogar/{id}/genai") @ResponseBody
    public EstadoPartidaDTO jogar(@PathVariable Long id) { return service.jogar(id); }
    @ExceptionHandler(IllegalStateException.class) @ResponseBody
    public ResponseEntity<?> falhaIA(IllegalStateException e) { return ResponseEntity.status(502).body(Map.of("mensagem", e.getMessage())); }
    @ExceptionHandler(IllegalArgumentException.class) @ResponseBody
    public ResponseEntity<?> invalido(IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("mensagem", e.getMessage())); }
    @ExceptionHandler(OptimisticLockingFailureException.class) @ResponseBody
    public ResponseEntity<?> conflito() { return ResponseEntity.status(409).body(Map.of("mensagem", "Outra jogada atualizou a partida. Recarregue a pagina.")); }
}
