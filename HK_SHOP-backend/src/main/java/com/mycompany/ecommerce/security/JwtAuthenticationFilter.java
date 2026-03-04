package com.mycompany.ecommerce.security;

import com.mycompany.ecommerce.user.User;
import com.mycompany.ecommerce.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/") || path.equals("/api/auth")
                || path.startsWith("/auth/") || path.equals("/auth");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // ✅ Ne pas skipper si c'est "anonymous"
            Authentication existing = SecurityContextHolder.getContext().getAuthentication();
            if (existing != null
                    && existing.isAuthenticated()
                    && !(existing instanceof AnonymousAuthenticationToken)) {
                filterChain.doFilter(request, response);
                return;
            }

            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = header.substring(7).trim();
            if (token.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtTokenProvider.validateToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            String subject = jwtTokenProvider.extractSubject(token); // subject = userId (String)
            if (subject == null || subject.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            Long userId;
            try {
                userId = Long.parseLong(subject);
            } catch (NumberFormatException e) {
                filterChain.doFilter(request, response);
                return;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.debug("User not found for id {}", userId);
                filterChain.doFilter(request, response);
                return;
            }

            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(user.getUsername())
                    .password(user.getPassword())
                    .authorities(
                            user.getRoles().stream()
                                    .map(role -> new SimpleGrantedAuthority(role.name())) // ✅ FIX (Role -> String)
                                    .collect(Collectors.toList())
                    )
                    .build();

            // ✅ principal = userId (Long) => AuditorAware<Long> va le récupérer
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            userDetails.getAuthorities()
                    );

            WebAuthenticationDetailsSource webDetails = new WebAuthenticationDetailsSource();
            auth.setDetails(new AuthDetails(user.getUsername(), webDetails.buildDetails(request)));

            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            log.error("Erreur JWT filter: {}", ex.getMessage(), ex);
            filterChain.doFilter(request, response);
        }
    }

    @Getter
    public static class AuthDetails {
        private final String username;
        private final Object webDetails;

        public AuthDetails(String username, Object webDetails) {
            this.username = username;
            this.webDetails = webDetails;
        }
    }
}