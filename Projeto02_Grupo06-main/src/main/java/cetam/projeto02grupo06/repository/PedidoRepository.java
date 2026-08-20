package cetam.projeto02grupo06.repository;

import cetam.projeto02grupo06.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    // JPQL: Busca todos os pedidos de um cliente específico
    @Query("SELECT p FROM Pedido p WHERE p.cliente.id = :clienteId")
    List<Pedido> buscarPedidosPorCliente(@Param("clienteId") Integer clienteId);

    // JPQL: Busca pedidos realizados dentro de um intervalo de datas
    @Query("SELECT p FROM Pedido p WHERE p.dataPedido BETWEEN :inicio AND :fim")
    List<Pedido> buscarVendasPorPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

}