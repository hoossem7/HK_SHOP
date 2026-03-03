package com.mycompany.ecommerce.auth;

/**
 * Réponse d'authentification
 */
public record AuthResponse(String accessToken, String tokenType) {
    public AuthResponse(String accessToken) { this(accessToken, "Bearer"); }
}
