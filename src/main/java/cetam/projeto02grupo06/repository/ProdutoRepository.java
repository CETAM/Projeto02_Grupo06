package cetam.projeto02grupo06.repository;

import cetam.projeto02grupo06.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Integer> {

    // JPQL: Busca produtos onde a quantidade no estoque seja menor ou igual a zero
    @Query("SELECT p FROM Produto p WHERE p.quantidadeEstoque <= 0")
    List<Produto> buscarProdutosEmFalta();

    // Derived query: conta quantos produtos estão vinculados a uma categoria
    long countByCategoriaId(Integer categoriaId);

}