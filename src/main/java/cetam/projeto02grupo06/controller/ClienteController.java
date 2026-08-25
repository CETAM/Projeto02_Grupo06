package cetam.projeto02grupo06.controller;

import cetam.projeto02grupo06.model.Cliente;
import cetam.projeto02grupo06.service.ClienteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public String listar(Model model) {

        model.addAttribute(
                "clientes",
                clienteService.listarTodos()
        );

        return "Clientes/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {

        model.addAttribute(
                "cliente",
                new Cliente()
        );

        return "Clientes/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Cliente cliente) {

        clienteService.salvar(cliente);

        return "redirect:/clientes";
    }

    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Integer id,
            Model model) {

        Cliente cliente =
                clienteService.buscarPorId(id);

        model.addAttribute(
                "cliente",
                cliente
        );

        return "Clientes/formulario";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable Integer id) {

        clienteService.excluir(id);

        return "redirect:/clientes";
    }
}