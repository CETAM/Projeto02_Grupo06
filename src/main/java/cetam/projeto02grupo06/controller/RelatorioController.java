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
        }

        return "Relatorios/pedidos-cliente";
    }
}