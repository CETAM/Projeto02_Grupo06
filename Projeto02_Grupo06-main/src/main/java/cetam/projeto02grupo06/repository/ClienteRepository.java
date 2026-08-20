package cetam.projeto02grupo06.repository;

import cetam.projeto02grupo06.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

}