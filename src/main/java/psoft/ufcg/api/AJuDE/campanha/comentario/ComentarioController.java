package psoft.ufcg.api.AJuDE.campanha.comentario;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import psoft.ufcg.api.AJuDE.auth.JwtService;
import psoft.ufcg.api.AJuDE.campanha.Campanha;
import psoft.ufcg.api.AJuDE.campanha.CampanhaService;
import psoft.ufcg.api.AJuDE.exceptions.ResourceNotFoundException;
import psoft.ufcg.api.AJuDE.exceptions.UnauthorizedException;
import psoft.ufcg.api.AJuDE.usuario.Usuario;

/**
 * 
 * @author Thayanne Luiza Victor Landim Sousa
 * @author Vítor Braga Diniz
 * 
 * @version 1.0
 */
@Api(value="comentarios")
@RestController
@RequestMapping("/campanhas/{campanhaIdURL}/comentarios")
public class ComentarioController {

	@Autowired
	ComentarioService comentarioService;

	@Autowired
	CampanhaService campanhaService;
	
	@Autowired
	JwtService jwtService;

	@ApiOperation(value = "Cria um novo comentário", notes = "Cria um comentário para uma campanha do sistema")
	@ApiResponses(value = {
					@ApiResponse(code = 200, message = "Comentario criado"),
					@ApiResponse(code = 401, message = "Usuário não autorizado")
	})
	@PostMapping("/")
	public ResponseEntity<ComentarioResponseDTO> create(@PathVariable String campanhaIdURL, @RequestBody ComentarioDTO comentarioDTO, 
			@RequestHeader("Authorization") String header) {
		Comentario comentario = validateComentario(comentarioDTO.get(), campanhaIdURL, header);
		return new ResponseEntity<ComentarioResponseDTO>(
				ComentarioResponseDTO.objToDTO(this.comentarioService.save(comentario)), 
				HttpStatus.CREATED
		);
	}

	@ApiOperation(value = "Recupera comentários", notes = "Recupera todos os comentários de uma única campanha")
	@ApiResponses(value = {
					@ApiResponse(code = 200, message = "Retorna comentários", response = List.class),
					@ApiResponse(code = 401, message = "Usuário não autorizado")
	})
	@GetMapping("/")
	public ResponseEntity<List<ComentarioResponseDTO>> getAll(@PathVariable String campanhaIdURL) {
		List<Comentario> comentarios = this.comentarioService.getAllByCampanhaId(campanhaIdURL);
		List<ComentarioResponseDTO> resposta = comentarios.stream()
															.filter(c -> c.getParent() == null)
															.map(c -> ComentarioResponseDTO.objToDTO(c))
															.collect(Collectors.toList());


		return new ResponseEntity<List<ComentarioResponseDTO>>(resposta, HttpStatus.OK);
	}

	@ApiOperation(value = "Recupera um único comentário", notes = "Recupera um comentário de uma única campanha")
	@ApiResponses(value = {
					@ApiResponse(code = 200, message = "Retorna comentário específico", response = Comentario.class),
					@ApiResponse(code = 404, message = "Comentário não encontrado")
	})
	@GetMapping("/{comentarioId}")
	public ResponseEntity<ComentarioResponseDTO> get(@PathVariable int comentarioId) {
		Optional<Comentario> comentario = this.comentarioService.findById(comentarioId);
		if (!comentario.isPresent()){
			throw new ResourceNotFoundException("Comentario não encontrado.");
		}
		return new ResponseEntity<ComentarioResponseDTO>(ComentarioResponseDTO.objToDTO(comentario.get()), 
														HttpStatus.OK);
	}

	@ApiOperation(value = "Deleta um comentário específico", notes = "Deleta um comentário específico de uma campanha")
	@ApiResponses(value = {
					@ApiResponse(code = 200, message = "Retorna comentário específico deletado", response = Comentario.class),
					@ApiResponse(code = 404, message = "Comentário não encontrado"),
					@ApiResponse(code = 401, message = "Usuário sem permissão para apagar comentário")
	})
	@DeleteMapping("/{comentarioId}")
	public ResponseEntity<ComentarioResponseDTO> delete(@PathVariable int comentarioId, @RequestHeader("Authorization") String header) {
		Optional<Comentario> comentario = this.comentarioService.deleteById(comentarioId);
		if (!comentario.isPresent()){
			throw new ResourceNotFoundException("Comentario não encontrado.");
		}
		if(!jwtService.usuarioHasPermission(header, comentario.get().getUsuario().getEmail()))
			throw new UnauthorizedException("Usuário não tem permissão para apagar esse comentário.");
		return new ResponseEntity<ComentarioResponseDTO>(ComentarioResponseDTO.objToDTO(comentario.get()), HttpStatus.OK);
	}

	@ApiOperation(value = "Cria resposta a um comentário", notes = "Cria resposta a um comentário específico de uma campanha")
	@ApiResponses(value = {
					@ApiResponse(code = 200, message = "Retorna resposta de comentário", response = Comentario.class),
					@ApiResponse(code = 404, message = "Comentário pai não encontrado"),
					@ApiResponse(code = 401, message = "Usuário sem permissão para responder comentário")
	})
	@PostMapping("/{comentarioId}")
	public ResponseEntity<ComentarioResponseDTO> create(@PathVariable String campanhaIdURL, @PathVariable int comentarioId, 
			@RequestBody ComentarioDTO comentarioDTO, @RequestHeader("Authorization") String header) {
		Comentario comentario = comentarioDTO.get();
		if (!this.comentarioService.findById(comentarioId).isPresent()){
			throw new ResourceNotFoundException("Comentario pai não encontrado.");
		}
		Comentario parent = this.comentarioService.findById(comentarioId).get();
		comentario = validateComentario(comentario, campanhaIdURL, header);		
		comentario.setParent(parent);
		parent.addResposta(comentario);
		Comentario newComentario = this.comentarioService.save(comentario);
		this.comentarioService.save(parent);
		return new ResponseEntity<ComentarioResponseDTO>(ComentarioResponseDTO.objToDTO(newComentario), HttpStatus.OK);
	}

	private Comentario validateComentario(Comentario comentario, String campanhaIdURL, String authorizationHeader) {
		Optional<Usuario> usuario = jwtService.getUsuarioByToken(authorizationHeader);
		if(!usuario.isPresent())
			throw new UnauthorizedException("Você precisa estar autenticado para criar uma nova campanha.");
		
		Campanha campanha = getCampanhaByIdURL(campanhaIdURL);
		comentario.setCampanha(campanha);
		comentario.setUsuario(usuario.get());
		return comentario;
	}

	/**
	 * Retorna um objeto campanha a partir de um identificador de URL. Caso não existe nenhuma campanha com esse identificador, 
	 * retorna um erro HTTP 404. 
	 * @param identificadorURL
	 * @return Campanha campanha com o Identificador de URL passado
	 */
	private Campanha getCampanhaByIdURL(String identificadorURL) {
		Optional<Campanha> campanha = this.campanhaService.findByIdURL(identificadorURL);
		
		if(!campanha.isPresent()) {
			throw new ResourceNotFoundException("Campanha não encontrada.");
		}

		return campanha.get();
	}
}
