package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.controller.dto.AuthResponse;
import com.Api.Fidelitypay.controller.dto.LoginRequest;
import com.Api.Fidelitypay.controller.dto.RegisterRequest;
import com.Api.Fidelitypay.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationService authService;

    public AuthController(AuthenticationService authService) {
        this.authService = authService;
    }

    /**
     * Inscription d'un nouvel utilisateur.
     * Le rôle par défaut est CLIENT.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        log.info("📝 Tentative d'inscription pour: {}", request.getEmail());

        String adminSecretHeader = httpRequest.getHeader("X-ADMIN-SECRET");

        try {
            AuthResponse response = authService.register(request, adminSecretHeader);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de l'inscription de {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("message", "Erreur serveur: " + e.getMessage()));
        }
    }

    /**
     * Connexion d'un utilisateur existant.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("🔑 Tentative de connexion pour: {}", request.getEmail());
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de la connexion de {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Une erreur interne est survenue"));
        }
    }
}
