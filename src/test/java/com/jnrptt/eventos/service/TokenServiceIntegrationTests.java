package com.jnrptt.eventos.service;

import com.jnrptt.eventos.TestContainersConfiguration;
import com.jnrptt.eventos.model.Rol;
import com.jnrptt.eventos.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestContainersConfiguration.class)
class TokenServiceIntegrationTests {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Value("${jwt.expiration}")
    private Duration expiration;

    @Test
    void tokenGeneradoSeDecodificaConSubYRoles() {
        Usuario usuario = new Usuario(1L, "Ana", "ana@example.com", "{noop}secreta", Rol.ORGANIZADOR);

        Jwt jwt = jwtDecoder.decode(tokenService.generar(usuario));

        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ORGANIZADOR");
    }

    @Test
    void cadaTokenSeEmiteEnElMomentoDeGenerarloYCaducaSegunLaExpiracion() throws InterruptedException {
        Usuario usuario = new Usuario(1L, "Ana", "ana@example.com", "{noop}secreta", Rol.ORGANIZADOR);

        Jwt primero = jwtDecoder.decode(tokenService.generar(usuario));
        Thread.sleep(1100);
        Jwt segundo = jwtDecoder.decode(tokenService.generar(usuario));

        assertThat(segundo.getIssuedAt()).isAfter(primero.getIssuedAt());
        assertThat(segundo.getExpiresAt()).isEqualTo(segundo.getIssuedAt().plus(expiration));
    }

    @Test
    void tokenCaducadoEsRechazadoAlDecodificar() {
        Instant emitido = Instant.now().minus(Duration.ofHours(2));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("eventos")
                .issuedAt(emitido)
                .expiresAt(emitido.plus(Duration.ofHours(1)))
                .subject("1")
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();

        assertThatThrownBy(() -> jwtDecoder.decode(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("expired");
    }
}
