package cetam.projeto02grupo06.service;

import cetam.projeto02grupo06.model.Cliente;
import cetam.projeto02grupo06.model.Pedido;
import cetam.projeto02grupo06.model.Produto;
import cetam.projeto02grupo06.repository.ClienteRepository;
import cetam.projeto02grupo06.repository.PedidoRepository;
import cetam.projeto02grupo06.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RelatorioService {

    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;

    // Construtor atualizado com as novas injeções
    public RelatorioService(ProdutoRepository produtoRepository,
                            PedidoRepository pedidoRepository,
                            ClienteRepository clienteRepository) {
        this.produtoRepository = produtoRepository;
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
    }

    // --- MÉTODOS DO ESTOQUE ---
    public List<Produto> buscarProdutosEmFalta() {
        return produtoRepository.buscarProdutosEmFalta();
    }

    // --- MÉTODOS DE PEDIDOS POR CLIENTE ---
    public List<Cliente> buscarTodosClientes() {
        return clienteRepository.findAll(); // Usado para montar o filtro na tela
    }

    public List<Pedido> buscarPedidosPorCliente(Integer clienteId) {
        return pedidoRepository.buscarPedidosPorCliente(clienteId); // Usado para montar a tabela
    }

    // --- METODO DE VENDAS POR PERÍODO ---
    public List<Pedido> buscarVendasPorPeriodo(java.time.LocalDateTime inicio, java.time.LocalDateTime fim) {
        return pedidoRepository.buscarVendasPorPeriodo(inicio, fim);
    }
}