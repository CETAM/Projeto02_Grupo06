package cetam.projeto02grupo06.service;

import cetam.projeto02grupo06.model.Categoria;
import cetam.projeto02grupo06.repository.CategoriaRepository;
import cetam.projeto02grupo06.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;


    public CategoriaService(CategoriaRepository categoriaRepository, ProdutoRepository produtoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
    }


    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }


    public Categoria buscarPorId(Integer id) {

        return categoriaRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Categoria não encontrada."
                        )
                );
    }


    public Categoria salvar(Categoria categoria) {

        if (categoria.getId() != null) {

            Categoria categoriaExistente =
                    categoriaRepository
                            .findById(categoria.getId())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Categoria não encontrada."
                                    )
                            );

            categoria.setDataCriacao(
                    categoriaExistente.getDataCriacao()
            );
        }

        return categoriaRepository.save(categoria);
    }


    // Quantos produtos estão vinculados a essa categoria
    public long contarProdutosVinculados(Integer categoriaId) {
        return produtoRepository.countByCategoriaId(categoriaId);
    }


    public void excluir(Integer id) {

        long produtosVinculados = contarProdutosVinculados(id);

        if (produtosVinculados > 0) {
            throw new IllegalStateException(
                    "Não é possível excluir esta categoria: existem "
                            + produtosVinculados
                            + " produto(s) vinculado(s) a ela."
            );
        }

        categoriaRepository.deleteById(id);
    }
}