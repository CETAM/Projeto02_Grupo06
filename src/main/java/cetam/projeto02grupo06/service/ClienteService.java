package cetam.projeto02grupo06.service;

import cetam.projeto02grupo06.model.Cliente;
import cetam.projeto02grupo06.repository.ClienteRepository;
import cetam.projeto02grupo06.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;

    public ClienteService(ClienteRepository clienteRepository, PedidoRepository pedidoRepository) {
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public Cliente buscarPorId(Integer id) {
        return clienteRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Cliente não encontrado."));
    }

    public Cliente salvar(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public void excluir(Integer id) {
        long pedidosVinculados = pedidoRepository.countByClienteId(id);
        if (pedidosVinculados > 0) {
            throw new IllegalStateException(
                    "Não é possível excluir este cliente: existem " + pedidosVinculados
                            + " pedido(s) vinculado(s) a ele.");
        }
        clienteRepository.deleteById(id);
    }
}