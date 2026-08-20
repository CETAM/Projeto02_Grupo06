package cetam.projeto02grupo06.model;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "controle_estoque")
public class ControleEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "quantidade_anterior")
    private Integer quantidadeAnterior;

    @Column(name = "quantidade_nova")
    private Integer quantidadeNova;

    @Column(name = "tipo_movimento", length = 50)
    private String tipoMovimento;

    @CreationTimestamp
    @Column(name = "data_movimento", nullable = false, updatable = false)
    private LocalDateTime dataMovimento;

    @Column(columnDefinition = "TEXT")
    private String observacoes;


    public ControleEstoque() {
    }


    public ControleEstoque(Produto produto, Integer quantidadeAnterior, Integer quantidadeNova,
                           String tipoMovimento, String observacoes) {
        this.produto = produto;
        this.quantidadeAnterior = quantidadeAnterior;
        this.quantidadeNova = quantidadeNova;
        this.tipoMovimento = tipoMovimento;
        this.observacoes = observacoes;
    }


    public Integer getId() {
        return id;
    }


    public void setId(Integer id) {
        this.id = id;
    }


    public Produto getProduto() {
        return produto;
    }


    public void setProduto(Produto produto) {
        this.produto = produto;
    }


    public Integer getQuantidadeAnterior() {
        return quantidadeAnterior;
    }


    public void setQuantidadeAnterior(Integer quantidadeAnterior) {
        this.quantidadeAnterior = quantidadeAnterior;
    }


    public Integer getQuantidadeNova() {
        return quantidadeNova;
    }


    public void setQuantidadeNova(Integer quantidadeNova) {
        this.quantidadeNova = quantidadeNova;
    }


    public String getTipoMovimento() {
        return tipoMovimento;
    }


    public void setTipoMovimento(String tipoMovimento) {
        this.tipoMovimento = tipoMovimento;
    }


    public LocalDateTime getDataMovimento() {
        return dataMovimento;
    }


    public void setDataMovimento(LocalDateTime dataMovimento) {
        this.dataMovimento = dataMovimento;
    }


    public String getObservacoes() {
        return observacoes;
    }


    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }


    public Integer calcularDiferenca() {
        if (quantidadeAnterior == null || quantidadeNova == null) {
            return null;
        }
        return quantidadeNova - quantidadeAnterior;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ControleEstoque that = (ControleEstoque) o;
        return Objects.equals(id, that.id);
    }


    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    @Override
    public String toString() {
        return "ControleEstoque{" +
                "id=" + id +
                ", produtoId=" + (produto != null ? produto.getId() : null) +
                ", quantidadeAnterior=" + quantidadeAnterior +
                ", quantidadeNova=" + quantidadeNova +
                ", tipoMovimento='" + tipoMovimento + '\'' +
                ", dataMovimento=" + dataMovimento +
                '}';
    }
}