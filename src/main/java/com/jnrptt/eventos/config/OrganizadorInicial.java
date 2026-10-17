package com.jnrptt.eventos.config;

import com.jnrptt.eventos.model.Usuario;
import com.jnrptt.eventos.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class OrganizadorInicial implements ApplicationRunner {

    private final UsuarioService usuarioService;
    private final String nombre;
    private final String email;
    private final String password;

    public OrganizadorInicial(UsuarioService usuarioService,
                              @Value("${organizador.nombre}") String nombre,
                              @Value("${organizador.email}") String email,
                              @Value("${organizador.password}") String password) {
        this.usuarioService = usuarioService;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            log.info("ORGANIZADOR_EMAIL u ORGANIZADOR_PASSWORD no definidos: no se crea el organizador inicial");
            return;
        }
        String emailNormalizado = Usuario.normalizarEmail(email);
        if (usuarioService.crearOrganizadorSiNoExiste(nombre, email, password)) {
            log.info("Organizador inicial creado con email {}", emailNormalizado);
        } else {
            log.info("Ya existe un usuario con email {}: no se modifica", emailNormalizado);
        }
    }
}
