package com.jnrptt.eventos.service;

import com.jnrptt.eventos.TestContainersConfiguration;
import com.jnrptt.eventos.model.Compra;
import com.jnrptt.eventos.model.EstadoCompra;
import com.jnrptt.eventos.model.Evento;
import com.jnrptt.eventos.model.Rol;
import com.jnrptt.eventos.model.TipoEntrada;
import com.jnrptt.eventos.model.TiposEntrada;
import com.jnrptt.eventos.model.Usuario;
import com.jnrptt.eventos.repository.CompraRepository;
import com.jnrptt.eventos.repository.EventoRepository;
import com.jnrptt.eventos.repository.TipoEntradaRepository;
import com.jnrptt.eventos.repository.UsuarioRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

// Cuenta las consultas SQL del listado para detectar el problema N+1.
// Usa su propio contexto porque necesita activar las estadísticas de Hibernate.
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestContainersConfiguration.class)
class CompraListadoConsultasIntegrationTests {

    private static final int COMPRAS = 5;

    @Autowired
    private CompraService compraService;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private TipoEntradaRepository tipoEntradaRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    @AfterEach
    void limpiaDatos() {
        compraRepository.deleteAll();
        eventoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void listarUnaPaginaDeComprasHaceUnaSolaConsulta() {
        var usuario = usuarioRepository.save(new Usuario(null, "Ana", "ana@example.com", "hash", Rol.USUARIO));
        // Cada compra con un tipo de entrada distinto: si todas compartieran tipo, la caché de primer
        // nivel lo cargaría una sola vez y el N+1 no se notaría.
        for (int i = 0; i < COMPRAS; i++) {
            var tipoEntrada = creaTipoEntrada("Concierto " + i);
            compraRepository.save(new Compra(null, 1, EstadoCompra.CONFIRMADA, LocalDateTime.now(),
                    tipoEntrada.getPrecio(), usuario, tipoEntrada));
        }
        Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estadisticas.clear();

        var pagina = compraService.listarDelUsuario(usuario.getId(), PageRequest.of(0, 10));

        assertThat(pagina.getContent()).hasSize(COMPRAS);
        assertThat(pagina.getContent()).allSatisfy(compra -> assertThat(compra.tipoEntrada()).isNotNull());
        // Una sola consulta: las compras con su tipo de entrada. Spring Data no lanza el count
        // porque la primera página ya trae menos elementos que el tamaño pedido.
        assertThat(estadisticas.getPrepareStatementCount())
                .as("Consultas SQL al listar %d compras", COMPRAS)
                .isEqualTo(1);
    }

    private TipoEntrada creaTipoEntrada(String nombreEvento) {
        var evento = eventoRepository.save(new Evento(null, nombreEvento, "Concierto de prueba",
                LocalDate.now().plusDays(30), "Madrid", "WiZink Center", new ArrayList<>()));
        return tipoEntradaRepository.save(new TipoEntrada(
                null, TiposEntrada.GENERAL, new BigDecimal("25.00"), 10, 10, evento));
    }
}
