package com.jnrptt.eventos.service;

import com.jnrptt.eventos.dto.LoginRequest;
import com.jnrptt.eventos.dto.RegistroRequest;
import com.jnrptt.eventos.exception.CredencialesInvalidasException;
import com.jnrptt.eventos.exception.EmailYaRegistradoException;
import com.jnrptt.eventos.exception.UsuarioNotFoundException;
import com.jnrptt.eventos.model.Rol;
import com.jnrptt.eventos.model.Usuario;
import com.jnrptt.eventos.repository.UsuarioRepository;
import com.jnrptt.eventos.validation.PoliticaPassword;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String hashFicticio;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException(id));
    }

    @Transactional
    public Usuario registrar(RegistroRequest registroRequest) {
        String email = Usuario.normalizarEmail(registroRequest.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailYaRegistradoException(email);
        }
        String passwordHash = passwordEncoder.encode(registroRequest.password());
        try {
            // saveAndFlush fuerza el INSERT aquí: si otro registro con el mismo email se ha colado
            // entre existsByEmail y este punto, la restricción unique salta dentro del try.
            return usuarioRepository.saveAndFlush(registroRequest.toUsuario(passwordHash));
        } catch (DataIntegrityViolationException e) {
            throw new EmailYaRegistradoException(email);
        }
    }

    @Transactional(readOnly = true)
    public Usuario autenticar(LoginRequest loginRequest) {
        String email = Usuario.normalizarEmail(loginRequest.email());
        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);
        String hash = usuario.map(Usuario::getPassword).orElse(hashFicticio);
        boolean passwordCorrecto = passwordEncoder.matches(loginRequest.password(), hash);
        if (usuario.isEmpty() || !passwordCorrecto) {
            throw new CredencialesInvalidasException();
        }
        return usuario.get();
    }

    @Transactional
    public boolean crearOrganizadorSiNoExiste(String nombre, String email, String password) {
        String emailNormalizado = Usuario.normalizarEmail(email);
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            return false;
        }
        validarPasswordOrganizador(password);
        String passwordHash = passwordEncoder.encode(password);
        usuarioRepository.save(new Usuario(null, nombre, emailNormalizado, passwordHash, Rol.ORGANIZADOR));
        return true;
    }

    private static void validarPasswordOrganizador(String password) {
        if (!PoliticaPassword.cumple(password)) {
            throw new IllegalStateException("Organizador inicial: " + PoliticaPassword.MENSAJE);
        }
    }
}
