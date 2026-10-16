package com.jnrptt.eventos.service;

import com.jnrptt.eventos.dto.LoginRequest;
import com.jnrptt.eventos.dto.RegistroRequest;
import com.jnrptt.eventos.exception.CredencialesInvalidasException;
import com.jnrptt.eventos.exception.EmailYaRegistradoException;
import com.jnrptt.eventos.exception.UsuarioNotFoundException;
import com.jnrptt.eventos.model.Usuario;
import com.jnrptt.eventos.repository.UsuarioRepository;
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
                .orElseThrow(() -> new CredencialesInvalidasException());
        if (!passwordEncoder.matches(loginRequest.password(), usuario.getPassword())) {
            throw new CredencialesInvalidasException();
        }
        return usuario;
    }
}
