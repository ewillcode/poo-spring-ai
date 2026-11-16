package br.edu.univille.poo.springai.repository;
import br.edu.univille.poo.springai.model.Jogada;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface JogadaRepository extends JpaRepository<Jogada, Long> {
    List<Jogada> findByPartidaIdOrderByOrdemAsc(Long partidaId);
}
