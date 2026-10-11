package com.jnrptt.eventos.controller;

import com.jnrptt.eventos.dto.CompraRequest;
import com.jnrptt.eventos.dto.CompraResponse;
import com.jnrptt.eventos.service.CompraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/compras")
public class CompraController {
    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @PostMapping
    public ResponseEntity<CompraResponse> createCompra(@Valid @RequestBody CompraRequest compraRequest) {
        CompraResponse response = compraService.comprar(compraRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
