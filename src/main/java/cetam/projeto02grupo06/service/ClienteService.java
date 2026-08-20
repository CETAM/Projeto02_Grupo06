package cetam.projeto02grupo06.service;

import cetam.projeto02grupo06.model.Cliente;
import cetam.projeto02grupo06.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;


    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }


    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }


    public Cliente buscarPorId(Integer id) {

        return clienteRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Cliente não encontrado."
                        )
                );
    }


    public Cliente salvar(Cliente cliente) {

        /*
         * Se estiver editando um cliente,
         * preserva a data original de cadastro.
         */
        if (cliente.getId() != null) {

            Cliente clienteExistente =
                    clienteRepository
                            .findById(cliente.getId())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Cliente não encontrado."
                                    )
                            );

            cliente.setDataCriacao(
                    clienteExistente.getDataCriacao()
            );
        }


        // Formata telefone
        cliente.setTelefone(
                formatarTelefone(cliente.getTelefone())
        );


        // Formata CEP
        cliente.setCep(
                formatarCep(cliente.getCep())
        );


        // Padroniza Estado
        cliente.setEstado(
                formatarEstado(cliente.getEstado())
        );


        return clienteRepository.save(cliente);
    }


    public void excluir(Integer id) {
        clienteRepository.deleteById(id);
    }


    // ==========================================
    // TELEFONE
    // 92991772667 -> (92) 991772667
    // ==========================================

    private String formatarTelefone(String telefone) {

        if (telefone == null || telefone.isBlank()) {
            return telefone;
        }

        String numeros =
                telefone.replaceAll("\\D", "");


        if (numeros.length() > 11) {
            numeros = numeros.substring(0, 11);
        }


        if (numeros.length() == 11) {

            return "("
                    + numeros.substring(0, 2)
                    + ") "
                    + numeros.substring(2);
        }


        return numeros;
    }


    // ==========================================
    // CEP
    // 69063490 -> 69063-490
    // ==========================================

    private String formatarCep(String cep) {

        if (cep == null || cep.isBlank()) {
            return cep;
        }

        String numeros =
                cep.replaceAll("\\D", "");


        if (numeros.length() > 8) {
            numeros = numeros.substring(0, 8);
        }


        if (numeros.length() == 8) {

            return numeros.substring(0, 5)
                    + "-"
                    + numeros.substring(5);
        }


        return numeros;
    }


    // ==========================================
    // ESTADO
    // am -> AM
    // ==========================================

    private String formatarEstado(String estado) {

        if (estado == null || estado.isBlank()) {
            return estado;
        }

        String estadoFormatado =
                estado
                        .replaceAll("[^a-zA-Z]", "")
                        .toUpperCase();


        if (estadoFormatado.length() > 2) {

            estadoFormatado =
                    estadoFormatado.substring(0, 2);
        }


        return estadoFormatado;
    }
}