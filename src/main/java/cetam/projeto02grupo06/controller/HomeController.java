package cetam.projeto02grupo06.controller;

import cetam.projeto02grupo06.service.RelatorioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final RelatorioService relatorioService;

    public HomeController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping({"/", "/index"})
    public String index(Model model) {

        // KPIs do topo do dashboard
        model.addAttribute("totalClientesAtivos", relatorioService.contarClientesAtivos());
        model.addAttribute("totalPedidosMes", relatorioService.contarPedidosNoMes());
        model.addAttribute("faturamentoMes", relatorioService.calcularFaturamentoNoMes());
        model.addAttribute("totalProdutosEmFalta", relatorioService.contarProdutosEmFalta());

        // Dados para o gráfico de vendas dos últimos 7 dias
        Map<String, java.math.BigDecimal> vendas7Dias = relatorioService.buscarVendasUltimos7Dias();

        List<String> chartLabels = new ArrayList<>(vendas7Dias.keySet());
        List<java.math.BigDecimal> chartValues = new ArrayList<>(vendas7Dias.values());

        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartValues", chartValues);

        return "Index";
    }
}