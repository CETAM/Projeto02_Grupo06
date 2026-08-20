package cetam.projeto02grupo06.repository;

import cetam.projeto02grupo06.model.ControleEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ControleEstoqueRepository extends JpaRepository<ControleEstoque, Integer> {

    List<ControleEstoque> findByProdutoIdOrderByDataMovimentoDesc(Integer produtoId);

    List<ControleEstoque> findByTipoMovimentoOrderByDataMovimentoDesc(String tipoMovimento);

    List<ControleEstoque> findByDataMovimentoBetweenOrderByDataMovimentoDesc(LocalDateTime dataInicio, LocalDateTime dataFim);

    List<ControleEstoque> findByProdutoIdAndTipoMovimentoOrderByDataMovimentoDesc(Integer produtoId, String tipoMovimento);

    Optional<ControleEstoque> findFirstByProdutoIdOrderByDataMovimentoDesc(Integer produtoId);

    List<ControleEstoque> findAllByOrderByDataMovimentoDesc();

    Long countByTipoMovimento(String tipoMovimento);

    Long countByProdutoId(Integer produtoId);

    @Query("SELECT c FROM ControleEstoque c WHERE c.produto.id = :produtoId AND c.dataMovimento >= :dataLimite ORDER BY c.dataMovimento DESC")
    List<ControleEstoque> findMovimentacoesRecentes(@Param("produtoId") Integer produtoId, @Param("dataLimite") LocalDateTime dataLimite);

    @Query("SELECT c FROM ControleEstoque c WHERE c.dataMovimento BETWEEN :dataInicio AND :dataFim AND c.tipoMovimento = :tipoMovimento ORDER BY c.dataMovimento DESC")
    List<ControleEstoque> findByPeriodoAndTipo(@Param("dataInicio") LocalDateTime dataInicio, @Param("dataFim") LocalDateTime dataFim, @Param("tipoMovimento") String tipoMovimento);
}

