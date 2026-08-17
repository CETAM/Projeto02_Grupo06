package cetam.projeto02grupo06.service;

import cetam.projeto02grupo06.model.Categoria;
import cetam.projeto02grupo06.repository.CategoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;


    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
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


    public void excluir(Integer id) {
        categoriaRepository.deleteById(id);
    }
}