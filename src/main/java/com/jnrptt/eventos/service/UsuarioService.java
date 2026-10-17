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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

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
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(CredencialesInvalidasException::new);
        if (!passwordEncoder.matches(loginRequest.password(), usuario.getPassword())) {
            throw new CredencialesInvalidasException();
        }
        return usuario;
    }

    /**
     * Crea un usuario ORGANIZADOR si no existe ninguno con ese email.
     * Si el email ya existe no se modifica nada (ni password ni rol).
     *
     * @return true si se ha creado el usuario, false si ya existía.
     */
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
