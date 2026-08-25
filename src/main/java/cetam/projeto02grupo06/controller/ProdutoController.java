package cetam.projeto02grupo06.controller;

import cetam.projeto02grupo06.model.Categoria;
import cetam.projeto02grupo06.model.Produto;
import cetam.projeto02grupo06.service.CategoriaService;
import cetam.projeto02grupo06.service.ProdutoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;

    public ProdutoController(ProdutoService produtoService, CategoriaService categoriaService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String listar(Model model) {

        model.addAttribute("produtos", produtoService.listarTodos());
        model.addAttribute("categorias", categoriaService.listarTodas());

        return "Produtos/lista";
    }

    @PostMapping("/salvar")
    public String salvar(
            @ModelAttribute Produto produto,
            @RequestParam("categoriaId") Integer categoriaId) {

        // Busca a categoria já persistida em vez de confiar no bind automático,
        // evitando que o Hibernate receba uma referência "solta" (transiente)
        Categoria categoria = categoriaService.buscarPorId(categoriaId);
        produto.setCategoria(categoria);

        produtoService.salvar(produto);

        return "redirect:/produtos";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable Integer id) {

        produtoService.excluir(id);

        return "redirect:/produtos";
    }
}
