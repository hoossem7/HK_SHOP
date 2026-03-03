package com.mycompany.ecommerce.auth;

/**
 * Requête de login
 */
public record LoginRequest(String usernameOrEmail, String password) {}
