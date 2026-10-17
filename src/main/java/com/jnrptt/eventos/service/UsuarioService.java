package com.jnrptt.eventos.service;

import com.jnrptt.eventos.dto.LoginRequest;
import com.jnrptt.eventos.dto.RegistroRequest;
import com.jnrptt.eventos.exception.CredencialesInvalidasException;
import com.jnrptt.eventos.exception.EmailYaRegistradoException;
import com.jnrptt.eventos.exception.UsuarioNotFoundException;
import com.jnrptt.eventos.model.Rol;
import com.jnrptt.eventos.model.Usuario;
import com.jnrptt.eventos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private static final int PASSWORD_MIN_CARACTERES = 8;
    // BCrypt solo admite hasta 72 bytes.
    private static final int PASSWORD_MAX_BYTES = 72;

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
                .orElseThrow(() -> new CredencialesInvalidasException());
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
        if (password == null
                || password.length() < PASSWORD_MIN_CARACTERES
                || password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
            // El mensaje nunca incluye el password.
            throw new IllegalStateException("El password del organizador inicial debe tener al menos "
                    + PASSWORD_MIN_CARACTERES + " caracteres y como máximo " + PASSWORD_MAX_BYTES + " bytes");
        }
    }
}
