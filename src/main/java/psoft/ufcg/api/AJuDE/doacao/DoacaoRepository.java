package psoft.ufcg.api.AJuDE.doacao;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoacaoRepository<T, ID extends Serializable> extends JpaRepository<Doacao, Long> {

	List<Doacao> findByCampanhaIdentificadorURL(String campanhaIdURL);

	List<Doacao> findByDoadorEmail(String email);
	
	List<Doacao> findByDoadorEmailAndCampanhaNomeContainsIgnoreCase(String email, String substring);
	
	List<Doacao> findByDoadorEmailAndCampanhaDescricaoContainsIgnoreCase(String email, String substring);

}
