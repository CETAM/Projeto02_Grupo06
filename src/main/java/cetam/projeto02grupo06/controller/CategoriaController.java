package cetam.projeto02grupo06.controller;

import cetam.projeto02grupo06.model.Categoria;
import cetam.projeto02grupo06.service.CategoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;


    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }


    @GetMapping
    public String listar(Model model) {

        model.addAttribute(
                "categorias",
                categoriaService.listarTodas()
        );

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
            @ModelAttribute Categoria categoria) {

        categoriaService.salvar(categoria);

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
            @PathVariable Integer id) {

        categoriaService.excluir(id);

        return "redirect:/categorias";
    }
}