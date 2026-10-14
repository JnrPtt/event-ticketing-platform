package com.jnrptt.eventos.service;

import com.jnrptt.eventos.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenService {

    @Value("${jwt.expiration}")
    private Duration expiration;
    private final JwtEncoder jwtEncoder;

    private Instant ahora = Instant.now();

    public String generar(Usuario usuario) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("eventos")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(expiration))
                .subject(String.valueOf(usuario.getId()))
                .claim("roles", List.of(usuario.getRol().name()))
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }
}
