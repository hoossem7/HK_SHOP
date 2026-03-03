package com.mycompany.ecommerce.auth;

import java.util.Set;

/**
 * Requête d'enregistrement
 */
public record RegisterRequest(String username, String email, String password, Set<String> roles) {}
