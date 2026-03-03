package com.mycompany.ecommerce.auth;

import com.mycompany.ecommerce.handler.BusinessErrorCodes;
import com.mycompany.ecommerce.handler.BusinessException;
import com.mycompany.ecommerce.security.JwtTokenProvider;
import com.mycompany.ecommerce.user.User;
import com.mycompany.ecommerce.user.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.refreshTokenCookieName:refreshToken}")
    private String refreshTokenCookieName;

    @Value("${app.refreshTokenCookieMaxAgeSeconds:2592000}")
    private Long refreshTokenCookieMaxAgeSeconds;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        // ✅ handlers: throw BusinessException
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(BusinessErrorCodes.USERNAME_TAKEN);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(BusinessErrorCodes.EMAIL_TAKEN);
        }

        Set<String> roles = request.roles();
        if (roles == null || roles.isEmpty()) {
            roles = new HashSet<>();
            roles.add("ROLE_USER");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(roles)
                .enabled(true)
                .build();

        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.usernameOrEmail(), loginRequest.password())
        );

        // ✅ Récupérer le user via username OU email (car usernameOrEmail peut être un email)
        String identifier = loginRequest.usernameOrEmail();

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new BusinessException(BusinessErrorCodes.USER_NOT_FOUND));

        // ✅ Générer token avec subject = userId
        String accessToken = jwtTokenProvider.generateTokenFromUser(user);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        Cookie cookie = new Cookie(refreshTokenCookieName, refreshToken.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(Math.toIntExact(refreshTokenCookieMaxAgeSeconds));
        cookie.setSecure(true); // en dev HTTP => mets false sinon cookie non stocké
        response.addCookie(cookie);

        response.setHeader(HttpHeaders.SET_COOKIE,
                cookie.getName() + "=" + cookie.getValue()
                        + "; HttpOnly; Path=/; Max-Age=" + cookie.getMaxAge()
                        + "; SameSite=Strict; Secure"
        );

        return ResponseEntity.ok(new AuthResponse(accessToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenValue,
            HttpServletResponse response
    ) {
        // ✅ handlers
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new BusinessException(BusinessErrorCodes.REFRESH_TOKEN_MISSING);
        }

        RefreshToken existing = refreshTokenService.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(BusinessErrorCodes.REFRESH_TOKEN_INVALID));

        // ✅ méthode ajoutée dans service pour matcher le controller
        Optional<RefreshToken> valid = refreshTokenService.verifyExpirationAndActive(existing);
        if (valid.isEmpty()) {
            throw new BusinessException(BusinessErrorCodes.REFRESH_TOKEN_INVALID);
        }

        // rotation
        refreshTokenService.revokeToken(existing);
        RefreshToken newRefresh = refreshTokenService.createRefreshToken(existing.getUser().getId());

        String newAccessToken = jwtTokenProvider.generateTokenFromUser(existing.getUser());

        Cookie cookie = new Cookie(refreshTokenCookieName, newRefresh.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(Math.toIntExact(refreshTokenCookieMaxAgeSeconds));
        cookie.setSecure(true); // true in prod with HTTPS (en dev: false)
        response.addCookie(cookie);

        response.setHeader(HttpHeaders.SET_COOKIE,
                cookie.getName() + "=" + cookie.getValue()
                        + "; HttpOnly; Path=/; Max-Age=" + cookie.getMaxAge()
                        + "; SameSite=Strict; Secure"
        );

        return ResponseEntity.ok(new AuthResponse(newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenValue,
            HttpServletResponse response
    ) {
        if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
            refreshTokenService.findByToken(refreshTokenValue).ifPresent(refreshTokenService::revokeToken);
        }

        Cookie deleteCookie = new Cookie(refreshTokenCookieName, "");
        deleteCookie.setHttpOnly(true);
        deleteCookie.setPath("/");
        deleteCookie.setMaxAge(0);
        deleteCookie.setSecure(true); // true in prod with HTTPS (en dev: false)
        response.addCookie(deleteCookie);

        response.setHeader(HttpHeaders.SET_COOKIE,
                deleteCookie.getName() + "=; HttpOnly; Path=/; Max-Age=0; SameSite=Strict; Secure"
        );

        return ResponseEntity.noContent().build();
    }
}