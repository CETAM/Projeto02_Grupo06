package cetam.projeto02grupo06.controller;

import cetam.projeto02grupo06.model.Cliente;
import cetam.projeto02grupo06.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

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
    public String salvar(
            @Valid @ModelAttribute Cliente cliente,
            BindingResult resultado,
            RedirectAttributes redirectAttributes) {

        if (resultado.hasErrors()) {
            String mensagens = resultado.getFieldErrors().stream()
                    .map(erro -> erro.getDefaultMessage())
                    .collect(Collectors.joining(" "));
            redirectAttributes.addFlashAttribute("erro", mensagens);
            return "redirect:/clientes";
        }

        try {
            clienteService.salvar(cliente);
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("erro", "Já existe um cliente cadastrado com esse e-mail.");
        }

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
    public String excluir(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            clienteService.excluir(id);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }

        return "redirect:/clientes";
    }
}