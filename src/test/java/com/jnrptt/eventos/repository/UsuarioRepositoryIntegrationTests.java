package com.jnrptt.eventos.repository;

import com.jnrptt.eventos.model.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class UsuarioRepositoryIntegrationTests {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    @AfterEach
    void limpiaUsuarios() {
        usuarioRepository.deleteAll();
    }

    @Test
    void guardaUnUsuarioYLoRecuperaPorEmail() {
        var guardado = usuarioRepository.save(usuario("Ana", "ana@example.com"));

        var encontrado = usuarioRepository.findByEmail("ana@example.com");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getId()).isEqualTo(guardado.getId());
        assertThat(encontrado.get().getNombre()).isEqualTo("Ana");
        assertThat(usuarioRepository.findByEmail("otro@example.com")).isEmpty();
    }

    @Test
    void noPermiteGuardarDosUsuariosConElMismoEmail() {
        usuarioRepository.save(usuario("Ana", "ana@example.com"));

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(usuario("Otro", "ana@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    private Usuario usuario(String nombre, String email) {
        return new Usuario(null, nombre, email);
    }
}
