package com.mycompany.ecommerce.auth;

public record ActivateAccountRequest(String email, String code) {}