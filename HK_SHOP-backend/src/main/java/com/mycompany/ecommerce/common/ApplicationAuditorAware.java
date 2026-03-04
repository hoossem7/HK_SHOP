package com.mycompany.ecommerce.common;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAware")
public class ApplicationAuditorAware implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return Optional.of(0L); // system
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof Long id) {
            return Optional.of(id);
        }

        if (principal instanceof String s) {
            try {
                return Optional.of(Long.parseLong(s));
            } catch (NumberFormatException ignored) {}
        }

        return Optional.of(0L);
    }
}