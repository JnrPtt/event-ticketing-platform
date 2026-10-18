package com.jnrptt.eventos.controller;

import com.jnrptt.eventos.dto.CompraRequest;
import com.jnrptt.eventos.dto.CompraResponse;
import com.jnrptt.eventos.service.CompraService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/compras")
@SecurityRequirement(name = "bearerAuth")
public class CompraController {
    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @PostMapping
    public ResponseEntity<CompraResponse> createCompra(@AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody CompraRequest compraRequest) {
        // El usuario sale del token, nunca del body: así nadie puede comprar en nombre de otro.
        CompraResponse response = compraService.comprar(usuarioId(jwt), compraRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/pagar")
    public CompraResponse pagar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return compraService.pagar(usuarioId(jwt), id);
    }

    @PostMapping("/{id}/cancelar")
    public CompraResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return compraService.cancelar(usuarioId(jwt), id);
    }

    // Solo se usan page y size: el orden lo fija el servicio (ver CompraService.listarDelUsuario).
    @GetMapping
    public PagedModel<CompraResponse> listar(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
        return new PagedModel<>(compraService.listarDelUsuario(usuarioId(jwt), pageable));
    }

    private Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
