package com.mycompany.ecommerce.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Mot de passe actuel obligatoire")
        String currentPassword,

        @NotBlank(message = "Nouveau mot de passe obligatoire")
        @Size(min = 6, message = "Nouveau mot de passe min 6 caractères")
        String newPassword,

        @NotBlank(message = "Confirmation obligatoire")
        @Size(min = 6, message = "Confirmation min 6 caractères")
        String confirmPassword
) {}