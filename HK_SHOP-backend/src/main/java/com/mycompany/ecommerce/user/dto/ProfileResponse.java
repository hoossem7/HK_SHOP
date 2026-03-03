package com.mycompany.ecommerce.user.dto;

import java.util.Set;

public record ProfileResponse(
        Long id,
        String username,
        String email,
        Set<String> roles
) {}