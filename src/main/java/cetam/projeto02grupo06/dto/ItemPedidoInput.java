package cetam.projeto02grupo06.dto;

/**
 * Representa um item enviado pelo modal de cadastro/edição de pedido.
 * O formulário serializa a lista de itens como JSON (campo "itensJson")
 * e o controller desserializa para esta estrutura antes de repassar ao service.
 */
public record ItemPedidoInput(Integer produtoId, Integer quantidade) {
}
