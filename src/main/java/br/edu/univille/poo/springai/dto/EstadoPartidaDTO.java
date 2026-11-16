package br.edu.univille.poo.springai.dto;
import java.util.List;
import br.edu.univille.poo.springai.model.ResultadoPartida;
public record EstadoPartidaDTO(Long id, String tema, String mascara, List<String> letras,
        int erros, int restantes, ResultadoPartida resultado, boolean finalizada, String mensagem, String palavra) {}
