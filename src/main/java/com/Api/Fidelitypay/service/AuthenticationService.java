package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.AuthResponse;
import com.Api.Fidelitypay.controller.dto.LoginRequest;
import com.Api.Fidelitypay.controller.dto.RegisterRequest;
import com.Api.Fidelitypay.model.User;
import com.Api.Fidelitypay.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final String adminCreationSecret;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            @Value("${admin.creation.secret:}") String adminCreationSecret) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.adminCreationSecret = adminCreationSecret;
    }

    /**
     * Inscription d'un nouvel utilisateur.
     * Par défaut, le rôle attribué est CLIENT.
     */
    public AuthResponse register(RegisterRequest request, String adminSecretHeader) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("⚠️ Tentative d'inscription avec un email déjà existant: {}", request.getEmail());
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        // Rôle par défaut: AUTRE (valeur générique quand aucun rôle précis n'est
        // fourni)
        User.Role role = User.Role.AUTRE;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                role = User.Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("❌ Rôle invalide demandé: {}", request.getRole());
                throw new IllegalArgumentException(
                        "Rôle invalide. Les rôles acceptés sont: ENTREPRENEUR_CEO, PRODUCT_MANAGER, AUTRE, DEVELOPER, ADMIN");
            }
        }

        // Sécurité pour la création de compte ADMIN
        if (role == User.Role.ADMIN) {
            if (adminCreationSecret == null || adminCreationSecret.isBlank()) {
                log.error("❌ Tentative de création d'ADMIN alors que la fonctionnalité est désactivée");
                throw new IllegalArgumentException("La création d'ADMIN n'est pas autorisée sur ce serveur");
            }
            if (adminSecretHeader == null || !adminSecretHeader.equals(adminCreationSecret)) {
                log.error("❌ Tentative de création d'ADMIN avec un secret invalide pour: {}", request.getEmail());
                throw new IllegalArgumentException(
                        "Secret d'administration invalide pour la création d'un compte ADMIN");
            }
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .applicationName(request.getApplicationName())
                .countries(request.getCountries())
                .callbackUrl(request.getCallbackUrl() != null ? request.getCallbackUrl() : "")
                .redirectUrl(request.getRedirectUrl() != null ? request.getRedirectUrl() : "")
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("✅ Nouvel utilisateur créé avec succès: {} (Rôle: {})", savedUser.getEmail(), savedUser.getRole());

        String jwtToken = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .token(jwtToken)
                .userId(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    /**
     * Connexion d'un utilisateur existant.
     */
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));
        } catch (Exception e) {
            log.warn("❌ Échec d'authentification pour: {}", request.getEmail());
            throw new IllegalArgumentException("Email ou mot de passe incorrect");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("❌ Utilisateur authentifié mais non trouvé en base: {}", request.getEmail());
                    return new IllegalArgumentException("Utilisateur non trouvé après authentification");
                });

        log.info("🔑 Connexion réussie pour: {} (Rôle: {})", user.getEmail(), user.getRole());

        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
