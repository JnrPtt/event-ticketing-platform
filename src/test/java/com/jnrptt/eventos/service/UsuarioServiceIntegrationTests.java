package com.jnrptt.eventos.service;

import com.jnrptt.eventos.TestContainersConfiguration;
import com.jnrptt.eventos.dto.RegistroRequest;
import com.jnrptt.eventos.exception.EmailYaRegistradoException;
import com.jnrptt.eventos.model.Rol;
import com.jnrptt.eventos.model.Usuario;
import com.jnrptt.eventos.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
@Import(TestContainersConfiguration.class)
class UsuarioServiceIntegrationTests {

    @Autowired
    private UsuarioService usuarioService;

    @MockitoSpyBean
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void limpiaUsuarios() {
        usuarioRepository.deleteAll();
    }

    @Test
    void emailDuplicadoQueSeCuelaTrasLaComprobacionLanzaEmailYaRegistrado() {
        usuarioService.registrar(registro("ana@example.com"));
        // Simula la carrera: otro registro guardó el email justo después de que este lo comprobara.
        doReturn(false).when(usuarioRepository).existsByEmail("ana@example.com");

        assertThatThrownBy(() -> usuarioService.registrar(registro("ana@example.com")))
                .isInstanceOf(EmailYaRegistradoException.class);
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void registrosConcurrentesConElMismoEmailSoloGuardanUno() throws Exception {
        int hilos = 10;
        var request = registro("ana@example.com");

        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            resultados.add(executor.submit(() -> {
                salida.await();
                try {
                    usuarioService.registrar(request);
                    return true;
                } catch (EmailYaRegistradoException e) {
                    return false;
                }
            }));
        }

        salida.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS))
                .as("Los registros concurrentes no terminaron a tiempo")
                .isTrue();

        int exitos = 0;
        for (Future<Boolean> resultado : resultados) {
            try {
                if (resultado.get()) {
                    exitos++;
                }
            } catch (ExecutionException e) {
                throw new AssertionError("Un registro lanzó una excepción inesperada", e.getCause());
            }
        }

        assertThat(exitos).isEqualTo(1);
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void crearOrganizadorValidoLoGuardaComoOrganizadorConElPasswordEnHash() {
        boolean creado = usuarioService.crearOrganizadorSiNoExiste("Org", "org@example.com", "secreto123");

        assertThat(creado).isTrue();
        Usuario guardado = usuarioRepository.findByEmail("org@example.com").orElseThrow();
        assertThat(guardado.getNombre()).isEqualTo("Org");
        assertThat(guardado.getRol()).isEqualTo(Rol.ORGANIZADOR);
        assertThat(guardado.getPassword()).startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches("secreto123", guardado.getPassword())).isTrue();
    }

    @Test
    void crearOrganizadorDosVecesSoloGuardaUno() {
        usuarioService.crearOrganizadorSiNoExiste("Org", "org@example.com", "secreto123");
        String hashOriginal = usuarioRepository.findByEmail("org@example.com").orElseThrow().getPassword();

        boolean creado = usuarioService.crearOrganizadorSiNoExiste("Otro", "org@example.com", "otroSecreto");

        assertThat(creado).isFalse();
        assertThat(usuarioRepository.count()).isEqualTo(1);
        Usuario guardado = usuarioRepository.findByEmail("org@example.com").orElseThrow();
        assertThat(guardado.getNombre()).isEqualTo("Org");
        assertThat(guardado.getPassword()).isEqualTo(hashOriginal);
    }

    @Test
    void crearOrganizadorConEmailDeUnUsuarioExistenteNoLoAsciende() {
        usuarioService.registrar(registro("ana@example.com"));

        boolean creado = usuarioService.crearOrganizadorSiNoExiste("Org", "ana@example.com", "otroSecreto");

        assertThat(creado).isFalse();
        assertThat(usuarioRepository.count()).isEqualTo(1);
        Usuario guardado = usuarioRepository.findByEmail("ana@example.com").orElseThrow();
        assertThat(guardado.getRol()).isEqualTo(Rol.USUARIO);
        assertThat(passwordEncoder.matches("secreto123", guardado.getPassword())).isTrue();
    }

    @Test
    void crearOrganizadorNormalizaElEmail() {
        usuarioService.crearOrganizadorSiNoExiste("Org", "  Org@Example.COM ", "secreto123");

        assertThat(usuarioRepository.findByEmail("org@example.com")).isPresent();

        boolean creado = usuarioService.crearOrganizadorSiNoExiste("Org", "org@example.com", "secreto123");
        assertThat(creado).isFalse();
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void crearOrganizadorConPasswordCortoLanzaIllegalStateYNoGuardaNada() {
        assertThatThrownBy(() -> usuarioService.crearOrganizadorSiNoExiste("Org", "org@example.com", "corto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("corto");

        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void crearOrganizadorConPasswordDeMasDe72BytesLanzaIllegalStateYNoGuardaNada() {
        // 37 caracteres pero 74 bytes en UTF-8.
        String password = "ñ".repeat(37);

        assertThatThrownBy(() -> usuarioService.crearOrganizadorSiNoExiste("Org", "org@example.com", password))
                .isInstanceOf(IllegalStateException.class);

        assertThat(usuarioRepository.count()).isZero();
    }

    private RegistroRequest registro(String email) {
        return new RegistroRequest("Ana", email, "secreto123");
    }
}
