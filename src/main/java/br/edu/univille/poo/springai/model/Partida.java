package br.edu.univille.poo.springai.model;
import jakarta.persistence.*;
@Entity
public class Partida {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version private Long versao;
    private String palavra;
    private String tema;
    private java.time.LocalDateTime dataHora;
    @Enumerated(EnumType.STRING)
    private ResultadoPartida resultado;
    private int erros;
    private int turnos;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPalavra() { return palavra; }
    public void setPalavra(String palavra) { this.palavra = palavra; }
    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }
    public java.time.LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(java.time.LocalDateTime dataHora) { this.dataHora = dataHora; }
    public ResultadoPartida getResultado() { return resultado; }
    public void setResultado(ResultadoPartida resultado) { this.resultado = resultado; }
    public int getErros() { return erros; }
    public void setErros(int erros) { this.erros = erros; }
    public int getTurnos() { return turnos; }
    public void setTurnos(int turnos) { this.turnos = turnos; }
}
