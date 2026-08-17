package cetam.projeto02grupo06.repository;

import cetam.projeto02grupo06.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository
        extends JpaRepository<Categoria, Integer> {

}