package com.jnrptt.eventos.service;

import com.jnrptt.eventos.dto.LoginRequest;
import com.jnrptt.eventos.exception.CredencialesInvalidasException;
import com.jnrptt.eventos.model.Rol;
import com.jnrptt.eventos.model.Usuario;
import com.jnrptt.eventos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Mide el tiempo real del login con el mismo PasswordEncoder que la aplicación (BCrypt).
// Si con un email inexistente no se ejecutara BCrypt, ese 401 tardaría microsegundos frente a
// decenas de milisegundos con un password incorrecto, y se podría averiguar qué emails existen.
// El umbral es amplio (la mitad) porque BCrypt domina el tiempo: sin el arreglo la diferencia
// es de varios órdenes de magnitud, así que el test no depende de la velocidad de la máquina.
class LoginTiempoDeRespuestaTest {

    private static final int CALENTAMIENTO = 3;
    private static final int MEDICIONES = 10;

    @Test
    void emailInexistenteTardaLoMismoQuePasswordIncorrecto() {
        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        var usuario = new Usuario(1L, "Ana", "ana@example.com", passwordEncoder.encode("secreto123"), Rol.USUARIO);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmail("nadie@example.com")).thenReturn(Optional.empty());
        var usuarioService = new UsuarioService(usuarioRepository, passwordEncoder);

        var passwordIncorrecto = new LoginRequest("ana@example.com", "otroPassword");
        var emailInexistente = new LoginRequest("nadie@example.com", "otroPassword");
        for (int i = 0; i < CALENTAMIENTO; i++) {
            medir(usuarioService, passwordIncorrecto);
            medir(usuarioService, emailInexistente);
        }

        long medianaPasswordIncorrecto = mediana(usuarioService, passwordIncorrecto);
        long medianaEmailInexistente = mediana(usuarioService, emailInexistente);

        assertThat(medianaEmailInexistente)
                .as("Login con email inexistente: %d µs; con password incorrecto: %d µs",
                        medianaEmailInexistente / 1_000, medianaPasswordIncorrecto / 1_000)
                .isGreaterThanOrEqualTo(medianaPasswordIncorrecto / 2);
    }

    private long mediana(UsuarioService usuarioService, LoginRequest login) {
        long[] tiempos = new long[MEDICIONES];
        for (int i = 0; i < MEDICIONES; i++) {
            tiempos[i] = medir(usuarioService, login);
        }
        Arrays.sort(tiempos);
        return tiempos[MEDICIONES / 2];
    }

    private long medir(UsuarioService usuarioService, LoginRequest login) {
        long inicio = System.nanoTime();
        assertThatThrownBy(() -> usuarioService.autenticar(login))
                .isInstanceOf(CredencialesInvalidasException.class);
        return System.nanoTime() - inicio;
    }
}
