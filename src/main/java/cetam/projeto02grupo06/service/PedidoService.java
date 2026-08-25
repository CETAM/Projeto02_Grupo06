package cetam.projeto02grupo06.service;

import cetam.projeto02grupo06.dto.ItemPedidoInput;
import cetam.projeto02grupo06.model.ItemPedido;
import cetam.projeto02grupo06.model.Pedido;
import cetam.projeto02grupo06.model.Produto;
import cetam.projeto02grupo06.repository.ItemPedidoRepository;
import cetam.projeto02grupo06.repository.PedidoRepository;
import cetam.projeto02grupo06.repository.ProdutoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;
    private final ProdutoRepository produtoRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                          ItemPedidoRepository itemPedidoRepository,
                          ProdutoRepository produtoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
        this.produtoRepository = produtoRepository;
    }

    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll(Sort.by(Sort.Direction.DESC, "dataPedido"));
    }

    public Pedido buscarPorId(Integer id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
    }

    public List<ItemPedido> buscarItensDoPedido(Integer pedidoId) {
        return itemPedidoRepository.findByPedidoId(pedidoId);
    }

    /**
     * Cria ou atualiza um pedido junto com seus itens.
     *
     * Regras de estoque:
     * - Ao editar, as quantidades dos itens antigos são devolvidas ao estoque
     *   antes de aplicar os itens novos (evita "vazamento" de estoque reservado).
     * - Cada item novo é validado contra o estoque disponível; se faltar produto,
     *   a operação inteira é revertida (transação) e uma mensagem clara é lançada.
     * - O valor total é sempre recalculado no servidor, nunca confiado ao formulário.
     */
    @Transactional
    public Pedido salvar(Pedido dadosPedido, List<ItemPedidoInput> itensInput) {

        if (itensInput == null || itensInput.isEmpty()) {
            throw new IllegalArgumentException("O pedido precisa ter ao menos um item.");
        }

        Pedido pedido;

        if (dadosPedido.getId() != null) {

            pedido = pedidoRepository.findById(dadosPedido.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));

            // Devolve ao estoque as quantidades dos itens antigos antes de aplicar os novos
            List<ItemPedido> itensAntigos = itemPedidoRepository.findByPedidoId(pedido.getId());

            for (ItemPedido itemAntigo : itensAntigos) {
                Produto produto = itemAntigo.getProduto();
                produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() + itemAntigo.getQuantidade());
                produtoRepository.save(produto);
            }

            if (!itensAntigos.isEmpty()) {
                itemPedidoRepository.deleteAll(itensAntigos);
            }

        } else {
            pedido = new Pedido();
        }

        pedido.setCliente(dadosPedido.getCliente());
        pedido.setStatus(dadosPedido.getStatus());
        pedido.setDataEntrega(dadosPedido.getDataEntrega());
        pedido.setObservacoes(dadosPedido.getObservacoes());

        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        List<ItemPedido> novosItens = new ArrayList<>();
        BigDecimal valorTotal = BigDecimal.ZERO;

        for (ItemPedidoInput itemInput : itensInput) {

            if (itemInput.produtoId() == null || itemInput.quantidade() == null || itemInput.quantidade() <= 0) {
                throw new IllegalArgumentException("Item de pedido inválido.");
            }

            Produto produto = produtoRepository.findById(itemInput.produtoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));

            if (produto.getQuantidadeEstoque() < itemInput.quantidade()) {
                throw new IllegalStateException(
                        "Estoque insuficiente para \"" + produto.getNome() + "\". Disponível: "
                                + produto.getQuantidadeEstoque() + " un.");
            }

            BigDecimal precoUnitario = produto.getPreco();
            BigDecimal subtotal = precoUnitario.multiply(BigDecimal.valueOf(itemInput.quantidade()));

            ItemPedido item = new ItemPedido();
            item.setPedido(pedidoSalvo);
            item.setProduto(produto);
            item.setQuantidade(itemInput.quantidade());
            item.setPrecoUnitario(precoUnitario);
            item.setSubtotal(subtotal);

            novosItens.add(item);
            valorTotal = valorTotal.add(subtotal);

            produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() - itemInput.quantidade());
            produtoRepository.save(produto);
        }

        itemPedidoRepository.saveAll(novosItens);

        pedidoSalvo.setValorTotal(valorTotal);
        return pedidoRepository.save(pedidoSalvo);
    }

    @Transactional
    public void excluir(Integer id) {

        List<ItemPedido> itens = itemPedidoRepository.findByPedidoId(id);

        // Devolve ao estoque tudo que estava reservado por este pedido
        for (ItemPedido item : itens) {
            Produto produto = item.getProduto();
            produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() + item.getQuantidade());
            produtoRepository.save(produto);
        }

        itemPedidoRepository.deleteAll(itens);
        pedidoRepository.deleteById(id);
    }
}
