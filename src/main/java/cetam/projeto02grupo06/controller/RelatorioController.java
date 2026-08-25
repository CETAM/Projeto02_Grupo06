package cetam.projeto02grupo06.controller;

import cetam.projeto02grupo06.service.RelatorioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    // CENTRAL DE RELATÓRIOS: hub com um cartão-resumo por relatório disponível
    @GetMapping
    public String central(Model model) {

        model.addAttribute("totalProdutosEmFalta", relatorioService.contarProdutosEmFalta());
        model.addAttribute("totalClientesCadastrados", relatorioService.contarClientesCadastrados());
        model.addAttribute("faturamento30Dias", relatorioService.calcularFaturamentoUltimos30Dias());

        return "Relatorios/central";
    }

    @GetMapping("/estoque")
    public String relatorioEstoque(Model model) {
        model.addAttribute("produtos", relatorioService.buscarProdutosEmFalta());
        return "Relatorios/estoque";
    }

    // NOVA ROTA: Relatório de Pedidos por Cliente
    @GetMapping("/pedidos-cliente")
    public String relatorioPedidosCliente(
            @RequestParam(name = "clienteId", required = false) Integer clienteId,
            Model model) {

        // 1. Sempre envia a lista de clientes para preencher o <select> do filtro
        model.addAttribute("clientes", relatorioService.buscarTodosClientes());

        // 2. Se o usuário escolheu um cliente no filtro, busca os pedidos dele
        if (clienteId != null) {

            model.addAttribute("pedidos", relatorioService.buscarPedidosPorCliente(clienteId));
            model.addAttribute("clienteSelecionado", clienteId); // Ajuda a manter a opção selecionada no <select>

            // Nome do cliente filtrado, para exibir no texto de contexto acima da tabela
            relatorioService.buscarTodosClientes().stream()
                    .filter(c -> c.getId().equals(clienteId))
                    .findFirst()
                    .ifPresent(c -> model.addAttribute("nomeClienteSelecionado", c.getNome()));
        }

        return "Relatorios/pedidos-cliente";
    }

    // NOVA ROTA: Relatório de Vendas por Período
    @GetMapping("/vendas-periodo")
    public String relatorioVendasPeriodo(
            @RequestParam(name = "dataInicio", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dataInicio,
            @RequestParam(name = "dataFim", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dataFim,
            Model model) {

        // Se o usuário preencheu as duas datas no filtro
        if (dataInicio != null && dataFim != null) {

            // Converte a data inicial para as 00:00:00 e a final para as 23:59:59
            java.time.LocalDateTime inicio = dataInicio.atStartOfDay();
            java.time.LocalDateTime fim = dataFim.atTime(java.time.LocalTime.MAX);

            model.addAttribute("pedidos", relatorioService.buscarVendasPorPeriodo(inicio, fim));

            // Devolve as datas para a tela para os campos não ficarem em branco após filtrar
            model.addAttribute("dataInicio", dataInicio);
            model.addAttribute("dataFim", dataFim);
        }

        return "Relatorios/vendas-periodo";
    }
}