package cetam.projeto02grupo06.controller;

import cetam.projeto02grupo06.model.ControleEstoque;
import cetam.projeto02grupo06.service.ControleEstoqueService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/controle-estoque")
public class ControleEstoqueController {

    private final ControleEstoqueService service;

    private static final String REDIRECT_LIST = "redirect:/controle-estoque";
    private static final String TEMPLATE_LIST = "controle-estoque/lista";
    private static final String TEMPLATE_FORM = "controle-estoque/formulario";
    private static final String TEMPLATE_RELATORIO = "controle-estoque/relatorio";


    public ControleEstoqueController(ControleEstoqueService service) {
        this.service = service;
    }


    @GetMapping
    public String listar(Model model) {
        List<ControleEstoque> controles = service.listarTodos();
        model.addAttribute("controles", controles);
        model.addAttribute("total", service.obterTotalMovimentacoes());
        model.addAttribute("entradas", service.obterTotalPorTipo("ENTRADA"));
        model.addAttribute("saidas", service.obterTotalPorTipo("SAÍDA"));
        model.addAttribute("ajustes", service.obterTotalPorTipo("AJUSTE"));
        return TEMPLATE_LIST;
    }


    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("controleEstoque", new ControleEstoque());
        return TEMPLATE_FORM;
    }


    @PostMapping("/salvar")
    public String salvar(@ModelAttribute ControleEstoque controleEstoque, RedirectAttributes attributes) {
        try {
            service.salvar(controleEstoque);
            attributes.addFlashAttribute("sucesso", "Controle salvo com sucesso");
            return REDIRECT_LIST;
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("erro", e.getMessage());
            return REDIRECT_LIST + "/novo";
        }
    }


    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes attributes) {
        try {
            ControleEstoque controle = service.buscarPorId(id);
            model.addAttribute("controleEstoque", controle);
            return TEMPLATE_FORM;
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("erro", e.getMessage());
            return REDIRECT_LIST;
        }
    }


    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable Integer id, RedirectAttributes attributes) {
        try {
            service.excluir(id);
            attributes.addFlashAttribute("sucesso", "Controle excluído com sucesso");
            return REDIRECT_LIST;
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("erro", e.getMessage());
            return REDIRECT_LIST;
        }
    }


    @GetMapping("/produto/{produtoId}")
    public String porProduto(@PathVariable Integer produtoId, Model model, RedirectAttributes attributes) {
        try {
            List<ControleEstoque> controles = service.buscarPorProduto(produtoId);
            Long total = service.obterTotalPorProduto(produtoId);
            model.addAttribute("controles", controles);
            model.addAttribute("produtoId", produtoId);
            model.addAttribute("total", total);
            return "controle-estoque/por-produto";
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("erro", e.getMessage());
            return REDIRECT_LIST;
        }
    }


    @GetMapping("/tipo/{tipoMovimento}")
    public String porTipo(@PathVariable String tipoMovimento, Model model, RedirectAttributes attributes) {
        try {
            List<ControleEstoque> controles = service.buscarPorTipo(tipoMovimento);
            Long total = service.obterTotalPorTipo(tipoMovimento);
            model.addAttribute("controles", controles);
            model.addAttribute("tipoMovimento", tipoMovimento);
            model.addAttribute("total", total);
            return "controle-estoque/por-tipo";
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("erro", e.getMessage());
            return REDIRECT_LIST;
        }
    }


    @GetMapping("/entrada/{produtoId}")
    public String formEntrada(@PathVariable Integer produtoId, Model model) {
        model.addAttribute("produtoId", produtoId);
        return "controle-estoque/entrada";
    }


    @PostMapping("/entrada/{produtoId}")
    public String registrarEntrada(@PathVariable Integer produtoId, @RequestParam Integer quantidade,
                                   @RequestParam(required = false) String observacoes, RedirectAttributes attributes) {
        try {
            service.registrarEntrada(produtoId, quantidade, observacoes);
            attributes.addFlashAttribute("sucesso", "Entrada registrada com sucesso");
            return REDIRECT_LIST;
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/controle-estoque/entrada/" + produtoId;
        }
    }


    @GetMapping("/saida/{produtoId}")
    public String formSaida(@PathVariable Integer produtoId, Model model) {
        model.addAttribute("produtoId", produtoId);
        return "controle-estoque/saida";
    }


    @PostMapping("/saida/{produtoId}")
    public String registrarSaida(@PathVariable Integer produtoId, @RequestParam Integer quantidade,
                                 @RequestParam(required = false) String observacoes, RedirectAttributes attributes) {
        try {
            service.registrarSaida(produtoId, quantidade, observacoes);
            attributes.addFlashAttribute("sucesso", "Saída registrada com sucesso");
            return REDIRECT_LIST;
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/controle-estoque/saida/" + produtoId;
        }
    }


    @GetMapping("/ajuste/{produtoId}")
    public String formAjuste(@PathVariable Integer produtoId, Model model) {
        model.addAttribute("produtoId", produtoId);
        return "controle-estoque/ajuste";
    }


    @PostMapping("/ajuste/{produtoId}")
    public String registrarAjuste(@PathVariable Integer produtoId, @RequestParam Integer novaQuantidade,
                                  @RequestParam(required = false) String observacoes, RedirectAttributes attributes) {
        try {
            service.registrarAjuste(produtoId, novaQuantidade, observacoes);
            attributes.addFlashAttribute("sucesso", "Ajuste registrado com sucesso");
            return REDIRECT_LIST;
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/controle-estoque/ajuste/" + produtoId;
        }
    }


    @GetMapping("/relatorio")
    public String relatorio(Model model) {
        List<ControleEstoque> controles = service.listarTodos();
        model.addAttribute("controles", controles);
        model.addAttribute("total", service.obterTotalMovimentacoes());
        model.addAttribute("entradas", service.obterTotalPorTipo("ENTRADA"));
        model.addAttribute("saidas", service.obterTotalPorTipo("SAÍDA"));
        model.addAttribute("ajustes", service.obterTotalPorTipo("AJUSTE"));
        return TEMPLATE_RELATORIO;
    }
}
