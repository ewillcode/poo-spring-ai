package br.edu.univille.poo.springai.model;
import jakarta.persistence.*;
@Entity
public class Jogada {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private Partida partida;
    private String letra;
    private boolean acerto;
    private int ordem;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLetra() { return letra; }
    public void setLetra(String letra) { this.letra = letra; }
    public boolean getAcerto() { return acerto; }
    public void setAcerto(boolean acerto) { this.acerto = acerto; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public Partida getPartida() { return partida; }
    public void setPartida(Partida partida) { this.partida = partida; }
}
