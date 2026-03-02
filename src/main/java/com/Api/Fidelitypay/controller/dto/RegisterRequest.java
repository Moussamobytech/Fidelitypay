package com.Api.Fidelitypay.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Le nom complet est obligatoire")
    private String fullName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    // Optional role: CLIENT, DEVELOPER, ADMIN. If omitted the service defaults to
    // CLIENT.
    private String role;

    // Application fields (for developer/application registration)
    private String applicationName;

    // List of ISO country codes or names
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> countries;

    private String callbackUrl;

    // Optional redirect URL after auth flow
    private String redirectUrl;
}
