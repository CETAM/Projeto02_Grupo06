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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
     * - O pedido é salvo em uma ÚNICA chamada a save(), com o valorTotal já
     *   calculado antes do INSERT. Isso evita um segundo save() (UPDATE) logo
     *   em seguida, que sobrescreveria com null o "data_entrega" preenchido
     *   pelo trigger trg_set_data_entrega do banco (esse era o TC-04 apontado
     *   pela suíte de testes).
     */
    @Transactional
    public Pedido salvar(Pedido dadosPedido, List<ItemPedidoInput> itensInput) {

        if (itensInput == null || itensInput.isEmpty()) {
            throw new IllegalArgumentException("O pedido precisa ter ao menos um item.");
        }

        // Agrupa itens repetidos do mesmo produto em uma única linha, somando
        // as quantidades, em vez de criar itens de pedido duplicados (TC-18)
        Map<Integer, Integer> quantidadePorProduto = new LinkedHashMap<>();
        for (ItemPedidoInput itemInput : itensInput) {
            if (itemInput.produtoId() == null || itemInput.quantidade() == null || itemInput.quantidade() <= 0) {
                throw new IllegalArgumentException("Item de pedido inválido.");
            }
            quantidadePorProduto.merge(itemInput.produtoId(), itemInput.quantidade(), Integer::sum);
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

        // Valida estoque e monta os itens ANTES de persistir o pedido,
        // para poder gravar o valorTotal já correto num único save()
        record ItemPreparado(Produto produto, Integer quantidade, BigDecimal subtotal) {}

        List<ItemPreparado> itensPreparados = new ArrayList<>();
        BigDecimal valorTotal = BigDecimal.ZERO;

        for (Map.Entry<Integer, Integer> entrada : quantidadePorProduto.entrySet()) {

            Produto produto = produtoRepository.findById(entrada.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));

            Integer quantidade = entrada.getValue();

            if (produto.getQuantidadeEstoque() < quantidade) {
                throw new IllegalStateException(
                        "Estoque insuficiente para \"" + produto.getNome() + "\". Disponível: "
                                + produto.getQuantidadeEstoque() + " un.");
            }

            BigDecimal subtotal = produto.getPreco().multiply(BigDecimal.valueOf(quantidade));

            itensPreparados.add(new ItemPreparado(produto, quantidade, subtotal));
            valorTotal = valorTotal.add(subtotal);
        }

        pedido.setValorTotal(valorTotal);

        // Único save(): o INSERT já sai com valorTotal correto, sem precisar
        // de um segundo save() que apagaria o data_entrega gerado pelo trigger
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        List<ItemPedido> novosItens = new ArrayList<>();

        for (ItemPreparado itemPreparado : itensPreparados) {

            ItemPedido item = new ItemPedido();
            item.setPedido(pedidoSalvo);
            item.setProduto(itemPreparado.produto());
            item.setQuantidade(itemPreparado.quantidade());
            item.setPrecoUnitario(itemPreparado.produto().getPreco());
            item.setSubtotal(itemPreparado.subtotal());
            novosItens.add(item);

            itemPreparado.produto().setQuantidadeEstoque(
                    itemPreparado.produto().getQuantidadeEstoque() - itemPreparado.quantidade());
            produtoRepository.save(itemPreparado.produto());
        }

        itemPedidoRepository.saveAll(novosItens);

        return pedidoSalvo;
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
