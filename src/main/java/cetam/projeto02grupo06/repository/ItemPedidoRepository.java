package cetam.projeto02grupo06.repository;

import cetam.projeto02grupo06.model.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Integer> {

    List<ItemPedido> findByPedidoId(Integer pedidoId);

    // Derived query: conta em quantos itens de pedido um produto aparece (usado para bloquear exclusão indevida)
    long countByProdutoId(Integer produtoId);

}