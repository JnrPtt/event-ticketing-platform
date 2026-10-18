package com.jnrptt.eventos.service;

import com.jnrptt.eventos.dto.LoginRequest;
import com.jnrptt.eventos.dto.RegistroRequest;
import com.jnrptt.eventos.exception.CredencialesInvalidasException;
import com.jnrptt.eventos.exception.EmailYaRegistradoException;
import com.jnrptt.eventos.model.Rol;
import com.jnrptt.eventos.model.Usuario;
import com.jnrptt.eventos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Test unitario de UsuarioService con el repositorio y el PasswordEncoder simulados.
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void autenticarNormalizaElEmailYDevuelveElUsuarioSiElPasswordCoincide() {
        var usuario = new Usuario(1L, "Ana", "ana@example.com", "hash", Rol.USUARIO);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secreto123", "hash")).thenReturn(true);

        var autenticado = usuarioService.autenticar(new LoginRequest("  ANA@Example.com ", "secreto123"));

        assertThat(autenticado).isSameAs(usuario);
    }

    @Test
    void autenticarConPasswordIncorrectoLanzaCredencialesInvalidas() {
        var usuario = new Usuario(1L, "Ana", "ana@example.com", "hash", Rol.USUARIO);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("otro", "hash")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.autenticar(new LoginRequest("ana@example.com", "otro")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void autenticarConEmailInexistenteLanzaLaMismaExcepcion() {
        when(usuarioRepository.findByEmail("nadie@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.autenticar(new LoginRequest("nadie@example.com", "secreto123")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    // Con un email inexistente también se ejecuta BCrypt (contra el hash ficticio), para que la
    // respuesta tarde lo mismo que con un password incorrecto. LoginTiempoDeRespuestaTest lo mide.
    @Test
    void autenticarConEmailInexistenteComparaIgualmenteElPassword() {
        when(usuarioRepository.findByEmail("nadie@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.autenticar(new LoginRequest("nadie@example.com", "secreto123")))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(passwordEncoder).matches(eq("secreto123"), any());
    }

    @Test
    void registrarGuardaElHashDelPasswordYNuncaElPasswordEnClaro() {
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secreto123")).thenReturn("{bcrypt}hash");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        usuarioService.registrar(new RegistroRequest("Ana", "ana@example.com", "secreto123"));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getPassword()).isEqualTo("{bcrypt}hash");
        assertThat(guardado.getValue().getRol()).isEqualTo(Rol.USUARIO);
    }

    @Test
    void registrarConEmailExistenteNoCalculaElHashNiGuarda() {
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar(new RegistroRequest("Ana", "ana@example.com", "secreto123")))
                .isInstanceOf(EmailYaRegistradoException.class);

        // encode se llama una vez al construir el servicio (hash ficticio), pero nunca con este password.
        verify(passwordEncoder, never()).encode("secreto123");
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    // Carrera: otro registro con el mismo email entra entre existsByEmail y el INSERT.
    // La restricción unique de la BD lo detecta y el servicio lo traduce a un 409, no a un 500.
    @Test
    void registrarTraduceLaViolacionDeUnicidadAEmailYaRegistrado() {
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secreto123")).thenReturn("{bcrypt}hash");
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> usuarioService.registrar(new RegistroRequest("Ana", "ana@example.com", "secreto123")))
                .isInstanceOf(EmailYaRegistradoException.class);
    }
}
