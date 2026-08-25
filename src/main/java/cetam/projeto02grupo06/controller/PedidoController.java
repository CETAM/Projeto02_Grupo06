package cetam.projeto02grupo06.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cetam.projeto02grupo06.dto.ItemPedidoInput;
import cetam.projeto02grupo06.model.Cliente;
import cetam.projeto02grupo06.model.ItemPedido;
import cetam.projeto02grupo06.model.Pedido;
import cetam.projeto02grupo06.repository.ClienteRepository;
import cetam.projeto02grupo06.service.PedidoService;
import cetam.projeto02grupo06.service.ProdutoService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final ProdutoService produtoService;
    private final ClienteRepository clienteRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PedidoController(PedidoService pedidoService,
                             ProdutoService produtoService,
                             ClienteRepository clienteRepository) {
        this.pedidoService = pedidoService;
        this.produtoService = produtoService;
        this.clienteRepository = clienteRepository;
    }

    @GetMapping
    public String listar(Model model) {

        List<Pedido> pedidos = pedidoService.listarTodos();

        model.addAttribute("pedidos", pedidos);
        model.addAttribute("clientes", clienteRepository.findAll(Sort.by(Sort.Direction.ASC, "nome")));
        model.addAttribute("produtos", produtoService.listarTodos());

        // Serializa os itens de cada pedido em JSON para alimentar o modal de edição
        // sem precisar de uma chamada AJAX extra ao abrir "Editar"
        Map<Integer, String> itensJsonPorPedido = new HashMap<>();
        Map<Integer, Integer> quantidadeItensPorPedido = new HashMap<>();

        for (Pedido pedido : pedidos) {

            List<ItemPedido> itens = pedidoService.buscarItensDoPedido(pedido.getId());
            List<Map<String, Object>> itensSimplificados = new java.util.ArrayList<>();

            for (ItemPedido item : itens) {
                Map<String, Object> mapa = new LinkedHashMap<>();
                mapa.put("produtoId", item.getProduto().getId());
                mapa.put("nome", item.getProduto().getNome());
                mapa.put("precoUnitario", item.getPrecoUnitario());
                mapa.put("quantidade", item.getQuantidade());
                itensSimplificados.add(mapa);
            }

            quantidadeItensPorPedido.put(pedido.getId(), itens.size());

            try {
                itensJsonPorPedido.put(pedido.getId(), objectMapper.writeValueAsString(itensSimplificados));
            } catch (JsonProcessingException e) {
                itensJsonPorPedido.put(pedido.getId(), "[]");
            }
        }

        model.addAttribute("itensJsonPorPedido", itensJsonPorPedido);
        model.addAttribute("quantidadeItensPorPedido", quantidadeItensPorPedido);

        return "Pedidos/lista";
    }

    @PostMapping("/salvar")
    public String salvar(
            @ModelAttribute Pedido pedido,
            @RequestParam("clienteId") Integer clienteId,
            @RequestParam("itensJson") String itensJson,
            RedirectAttributes redirectAttributes) {

        try {

            Cliente cliente = clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
            pedido.setCliente(cliente);

            List<ItemPedidoInput> itens = objectMapper.readValue(
                    itensJson,
                    new TypeReference<List<ItemPedidoInput>>() {
                    });

            pedidoService.salvar(pedido, itens);

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        } catch (JsonProcessingException e) {
            redirectAttributes.addFlashAttribute("erro", "Não foi possível processar os itens do pedido.");
        }

        return "redirect:/pedidos";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            pedidoService.excluir(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }

        return "redirect:/pedidos";
    }
}
