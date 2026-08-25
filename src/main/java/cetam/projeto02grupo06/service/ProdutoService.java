package cetam.projeto02grupo06.service;

import cetam.projeto02grupo06.model.Produto;
import cetam.projeto02grupo06.repository.ProdutoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public List<Produto> listarTodos() {
        return produtoRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
    }

    public Produto buscarPorId(Integer id) {
        return produtoRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Produto não encontrado."));
    }

    public Produto salvar(Produto produto) {

        if (produto.getId() != null) {

            Produto produtoExistente = produtoRepository.findById(produto.getId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Produto não encontrado."));

            // Preserva a data de criação original ao editar
            produto.setDataCriacao(produtoExistente.getDataCriacao());

        } else {
            produto.setDataCriacao(LocalDate.now());
        }

        if (produto.getQuantidadeEstoque() == null) {
            produto.setQuantidadeEstoque(0);
        }

        return produtoRepository.save(produto);
    }

    public void excluir(Integer id) {
        produtoRepository.deleteById(id);
    }
}
