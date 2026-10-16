package com.jnrptt.eventos.controller;

import com.jnrptt.eventos.dto.LoginRequest;
import com.jnrptt.eventos.dto.LoginResponse;
import com.jnrptt.eventos.dto.RegistroRequest;
import com.jnrptt.eventos.dto.UsuarioResponse;
import com.jnrptt.eventos.model.Usuario;
import com.jnrptt.eventos.service.TokenService;
import com.jnrptt.eventos.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UsuarioService usuarioService;
    private final TokenService tokenService;

    public AuthController(UsuarioService usuarioService, TokenService tokenService) {
        this.usuarioService = usuarioService;
        this.tokenService = tokenService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest registroRequest) {
        UsuarioResponse response = UsuarioResponse.from(usuarioService.registrar(registroRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> autenticar(@Valid @RequestBody LoginRequest loginRequest) {
        Usuario usuario = usuarioService.autenticar(loginRequest);
        return ResponseEntity.ok(LoginResponse.from(tokenService.generar(usuario), tokenService.getExpiracion()));
    }
}
