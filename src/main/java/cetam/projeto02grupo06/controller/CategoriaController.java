package cetam.projeto02grupo06.controller;

import cetam.projeto02grupo06.model.Categoria;
import cetam.projeto02grupo06.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;


    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }


    @GetMapping
    public String listar(Model model) {

        var categorias = categoriaService.listarTodas();

        // Monta um mapa {categoriaId -> quantidade de produtos vinculados}
        // usado para exibir o badge na tabela e bloquear exclusões indevidas
        Map<Integer, Long> contagemProdutos = new HashMap<>();
        for (Categoria categoria : categorias) {
            contagemProdutos.put(
                    categoria.getId(),
                    categoriaService.contarProdutosVinculados(categoria.getId())
            );
        }

        model.addAttribute("categorias", categorias);
        model.addAttribute("contagemProdutos", contagemProdutos);

        return "Categorias/lista";
    }


    @GetMapping("/novo")
    public String novo(Model model) {

        model.addAttribute(
                "categoria",
                new Categoria()
        );

        return "Categorias/formulario";
    }


    @PostMapping("/salvar")
    public String salvar(
            @Valid @ModelAttribute Categoria categoria,
            BindingResult resultado,
            RedirectAttributes redirectAttributes) {

        if (resultado.hasErrors()) {
            String mensagens = resultado.getFieldErrors().stream()
                    .map(erro -> erro.getDefaultMessage())
                    .collect(Collectors.joining(" "));
            redirectAttributes.addFlashAttribute("erro", mensagens);
            return "redirect:/categorias";
        }

        try {
            categoriaService.salvar(categoria);
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("erro", "Já existe uma categoria cadastrada com esse nome.");
        }

        return "redirect:/categorias";
    }


    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Integer id,
            Model model) {

        Categoria categoria =
                categoriaService.buscarPorId(id);

        model.addAttribute(
                "categoria",
                categoria
        );

        return "Categorias/formulario";
    }


    @PostMapping("/excluir/{id}")
    public String excluir(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            categoriaService.excluir(id);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }

        return "redirect:/categorias";
    }
}