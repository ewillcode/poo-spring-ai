package br.edu.univille.poo.springai.service;
import br.edu.univille.poo.springai.model.*;
import br.edu.univille.poo.springai.dto.EstadoPartidaDTO;
import br.edu.univille.poo.springai.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
@Service
public class ForcaService {
    private final PartidaRepository partidas;
    private final JogadaRepository jogadas;
    private final GeminiService gemini;
    public ForcaService(PartidaRepository partidas, JogadaRepository jogadas, GeminiService gemini) {
        this.partidas = partidas; this.jogadas = jogadas; this.gemini = gemini;
    }
    public static String normalizar(String valor) {
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
    }
    public Partida iniciar(String palavra, String tema) {
        String normalizada = normalizar(palavra);
        if (!normalizada.matches("[A-Z]{2,30}") || !palavra.trim().matches("[\\p{L}]{2,30}"))
            throw new IllegalArgumentException("Use uma palavra de 2 a 30 letras, sem espaços ou símbolos.");
        if (tema.isBlank() || tema.length() > 80 || tema.contains("\n") || tema.contains("\r"))
            throw new IllegalArgumentException("Informe um tema de 1 a 80 caracteres em uma linha.");
        Partida p = new Partida(); p.setPalavra(normalizada); p.setTema(tema.trim());
        p.setDataHora(LocalDateTime.now()); p.setResultado(ResultadoPartida.EM_ANDAMENTO);
        return partidas.save(p);
    }
    public List<Partida> listar() { return partidas.findAll(Sort.by(Sort.Direction.DESC, "dataHora")); }
    public Partida obter(Long id) {
        return partidas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partida não encontrada."));
    }
    public EstadoPartidaDTO estado(Long id) { return montarEstado(obter(id), jogadas.findByPartidaIdOrderByOrdemAsc(id)); }
    private EstadoPartidaDTO montarEstado(Partida p, List<Jogada> historico) {
        List<String> letras = historico.stream().map(Jogada::getLetra).toList();
        StringBuilder mascara = new StringBuilder();
        for (char c : p.getPalavra().toCharArray()) mascara.append(letras.contains(String.valueOf(c)) ? c : '_');
        boolean fim = p.getResultado() != ResultadoPartida.EM_ANDAMENTO;
        String mensagem = switch(p.getResultado()) {
            case VITORIA_IA -> "A IA descobriu a palavra e venceu!";
            case DERROTA_IA -> "A IA perdeu: cinco erros!";
            default -> "Peça o próximo palpite da IA.";
        };
        return new EstadoPartidaDTO(p.getId(), p.getTema(), mascara.toString(), letras, p.getErros(),
                5 - p.getErros(), p.getResultado(), fim, mensagem, fim ? p.getPalavra() : null);
    }
    @Transactional
    public EstadoPartidaDTO jogar(Long id) {
        return jogar(id, mensagem -> {});
    }
    @Transactional
    public EstadoPartidaDTO jogar(Long id, Consumer<String> progresso) {
        Partida p = obter(id);
        if (p.getResultado() != ResultadoPartida.EM_ANDAMENTO) throw new IllegalArgumentException("Partida encerrada.");
        List<Jogada> historico = new ArrayList<>(jogadas.findByPartidaIdOrderByOrdemAsc(id));
        EstadoPartidaDTO atual = montarEstado(p, historico);
        String letra = gemini.escolherLetra(p.getTema(), atual.mascara(), atual.letras(), progresso);
        Jogada j = new Jogada(); j.setPartida(p); j.setLetra(letra); j.setOrdem(p.getTurnos()+1);
        j.setAcerto(p.getPalavra().contains(letra));
        if (!j.getAcerto()) p.setErros(p.getErros()+1);
        p.setTurnos(p.getTurnos()+1); historico.add(j);
        if (!montarEstado(p, historico).mascara().contains("_")) p.setResultado(ResultadoPartida.VITORIA_IA);
        else if (p.getErros() == 5) p.setResultado(ResultadoPartida.DERROTA_IA);
        jogadas.save(j); partidas.saveAndFlush(p);
        return montarEstado(p, historico);
    }
}
