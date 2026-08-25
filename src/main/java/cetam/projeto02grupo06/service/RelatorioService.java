package cetam.projeto02grupo06.service;

import cetam.projeto02grupo06.model.Cliente;
import cetam.projeto02grupo06.model.Pedido;
import cetam.projeto02grupo06.model.Produto;
import cetam.projeto02grupo06.repository.ClienteRepository;
import cetam.projeto02grupo06.repository.PedidoRepository;
import cetam.projeto02grupo06.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public List<Pedido> buscarVendasPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return pedidoRepository.buscarVendasPorPeriodo(inicio, fim);
    }

    // --- MÉTODOS DO DASHBOARD (PÁGINA INICIAL) ---

    public long contarClientesAtivos() {
        return clienteRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getAtivo()))
                .count();
    }

    public long contarPedidosNoMes() {
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMes = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
        return pedidoRepository.buscarVendasPorPeriodo(inicioMes, fimMes).size();
    }

    public BigDecimal calcularFaturamentoNoMes() {
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMes = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

        return pedidoRepository.buscarVendasPorPeriodo(inicioMes, fimMes).stream()
                .map(Pedido::getValorTotal)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int contarProdutosEmFalta() {
        return produtoRepository.buscarProdutosEmFalta().size();
    }

    // --- MÉTODOS DA CENTRAL DE RELATÓRIOS (cartões-resumo do hub) ---

    public long contarClientesCadastrados() {
        return clienteRepository.count();
    }

    // Faturamento somado nos últimos 30 dias (usado no cartão-resumo de Vendas por Período)
    public BigDecimal calcularFaturamentoUltimos30Dias() {
        LocalDateTime inicio = LocalDate.now().minusDays(29).atStartOfDay();
        LocalDateTime fim = LocalDate.now().atTime(23, 59, 59);

        return pedidoRepository.buscarVendasPorPeriodo(inicio, fim).stream()
                .map(Pedido::getValorTotal)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Vendas dos últimos 7 dias (para o gráfico do dashboard)
    public Map<String, BigDecimal> buscarVendasUltimos7Dias() {
        LocalDateTime inicio = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime fim = LocalDate.now().atTime(23, 59, 59);

        List<Pedido> pedidos = pedidoRepository.buscarVendasPorPeriodo(inicio, fim);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        List<LocalDate> dias = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            dias.add(LocalDate.now().minusDays(i));
        }

        // Mapa temporario para somar os valores de cada dia
        Map<LocalDate, BigDecimal> somaPorDia = dias.stream()
                .collect(Collectors.toMap(dia -> dia, dia -> BigDecimal.ZERO, (a, b) -> a, java.util.LinkedHashMap::new));

        for (Pedido pedido : pedidos) {
            if (pedido.getDataPedido() == null || pedido.getValorTotal() == null) {
                continue;
            }
            LocalDate diaPedido = pedido.getDataPedido().toLocalDate();
            somaPorDia.merge(diaPedido, pedido.getValorTotal(), BigDecimal::add);
        }

        // Monta o resultado final ja formatado e na ordem cronologica correta
        Map<String, BigDecimal> resultado = new java.util.LinkedHashMap<>();
        for (LocalDate dia : dias) {
            resultado.put(dia.format(formatter), somaPorDia.getOrDefault(dia, BigDecimal.ZERO));
        }

        return resultado;
    }
}