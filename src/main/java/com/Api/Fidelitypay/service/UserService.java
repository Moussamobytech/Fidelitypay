package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.UserResponse;
import com.Api.Fidelitypay.controller.dto.UserUpdateRequest;
import com.Api.Fidelitypay.model.User;
import com.Api.Fidelitypay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse updateUser(String id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + id));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getEmail() != null) {
            if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Cet email est déjà utilisé par un autre utilisateur.");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getRole() != null) {
            try {
                user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Rôle invalide: " + request.getRole());
            }
        }
        if (request.getApplicationName() != null) user.setApplicationName(request.getApplicationName());
        if (request.getCountries() != null) user.setCountries(request.getCountries());
        if (request.getCallbackUrl() != null) user.setCallbackUrl(request.getCallbackUrl());
        if (request.getRedirectUrl() != null) user.setRedirectUrl(request.getRedirectUrl());
        if (request.getIsActive() != null) user.setActive(request.getIsActive());

        User updatedUser = userRepository.save(user);
        log.info("📝 Utilisateur {} mis à jour", id);
        return mapToUserResponse(updatedUser);
    }

    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + id);
        }
        userRepository.deleteById(id);
        log.info("🗑️ Utilisateur {} supprimé", id);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .applicationName(user.getApplicationName())
                .countries(user.getCountries())
                .callbackUrl(user.getCallbackUrl())
                .redirectUrl(user.getRedirectUrl())
                .createdAt(user.getCreatedAt())
                .isActive(user.isActive())
                .build();
    }
}
