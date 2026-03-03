package com.mycompany.ecommerce.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Username obligatoire")
        @Size(min = 3, max = 100, message = "Username doit contenir au moins 3 caractères")
        String username,

        @NotBlank(message = "Email obligatoire")
        @Email(message = "Email invalide")
        String email
) {}