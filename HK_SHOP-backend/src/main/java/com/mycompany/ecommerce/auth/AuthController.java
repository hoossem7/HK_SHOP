package com.mycompany.ecommerce.auth;

import com.mycompany.ecommerce.handler.BusinessErrorCodes;
import com.mycompany.ecommerce.handler.BusinessException;
import com.mycompany.ecommerce.security.JwtTokenProvider;
import com.mycompany.ecommerce.user.Role;
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

    private final AccountActivationService accountActivationService;

    @Value("${app.refreshTokenCookieName:refreshToken}")
    private String refreshTokenCookieName;

    @Value("${app.refreshTokenCookieMaxAgeSeconds:2592000}")
    private Long refreshTokenCookieMaxAgeSeconds;

    @Value("${app.cookieSecure:false}")
    private boolean cookieSecure;

    // =========================
    // REGISTER
    // =========================
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {

        // 1) EMAIL déjà existant ?
        Optional<User> byEmail = userRepository.findByEmail(request.email());
        if (byEmail.isPresent()) {
            User existing = byEmail.get();

            // ✅ si déjà activé => vrai conflit
            if (existing.isEnabled()) {
                throw new BusinessException(BusinessErrorCodes.EMAIL_TAKEN);
            }

            // ✅ compte NON activé => on renvoie juste un nouveau code
            accountActivationService.resendCodeFor(existing);

            return ResponseEntity.ok(
                    "Ce compte existe déjà mais n’est pas activé. Un nouveau code a été envoyé par email."
            );
        }

        // 2) USERNAME déjà existant ?
        Optional<User> byUsername = userRepository.findByUsername(request.username());
        if (byUsername.isPresent()) {
            User existing = byUsername.get();

            if (existing.isEnabled()) {
                throw new BusinessException(BusinessErrorCodes.USERNAME_TAKEN);
            }

            accountActivationService.resendCodeFor(existing);

            return ResponseEntity.ok(
                    "Ce compte existe déjà mais n’est pas activé. Un nouveau code a été envoyé par email."
            );
        }

        // 3) Création user disabled + rôle par défaut (sécurisé côté backend)
        Set<Role> roles = new HashSet<>();
        roles.add(Role.ROLE_CUSTOMER);

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(roles)
                .enabled(false)
                .build();

        userRepository.save(user);

        // ✅ send code
        accountActivationService.resendCodeFor(user);

        return ResponseEntity.ok("Compte créé. Un code d’activation a été envoyé par email.");
    }

    // =========================
    // ACTIVATE (CODE)
    // =========================
    @PostMapping("/activate")
    public ResponseEntity<String> activate(@RequestBody ActivateAccountRequest request) {
        accountActivationService.activateByCode(request.email(), request.code());
        return ResponseEntity.ok("Account activated");
    }

    // =========================
    // RESEND CODE (si pas reçu)
    // =========================
    @PostMapping("/resend-activation-code")
    public ResponseEntity<String> resendActivationCode(@RequestParam String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(BusinessErrorCodes.USER_NOT_FOUND));

        if (user.isEnabled()) {
            return ResponseEntity.ok("Compte déjà activé. Vous pouvez vous connecter.");
        }

        accountActivationService.resendCodeFor(user);
        return ResponseEntity.ok("Un nouveau code a été envoyé par email.");
    }

    // =========================
    // LOGIN
    // =========================
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.usernameOrEmail(), loginRequest.password())
        );

        String identifier = loginRequest.usernameOrEmail();

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new BusinessException(BusinessErrorCodes.USER_NOT_FOUND));

        if (!user.isEnabled()) {
            // ✅ message clair côté front : "compte non activé"
            throw new BusinessException(BusinessErrorCodes.ACCOUNT_DISABLED);
        }

        String accessToken = jwtTokenProvider.generateTokenFromUser(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        Cookie cookie = new Cookie(refreshTokenCookieName, refreshToken.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(Math.toIntExact(refreshTokenCookieMaxAgeSeconds));
        cookie.setSecure(cookieSecure);
        response.addCookie(cookie);

        response.setHeader(HttpHeaders.SET_COOKIE,
                cookie.getName() + "=" + cookie.getValue()
                        + "; HttpOnly; Path=/; Max-Age=" + cookie.getMaxAge()
                        + "; SameSite=Strict"
                        + (cookieSecure ? "; Secure" : "")
        );

        return ResponseEntity.ok(new AuthResponse(accessToken));
    }

    // =========================
    // REFRESH
    // =========================
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenValue,
            HttpServletResponse response
    ) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new BusinessException(BusinessErrorCodes.REFRESH_TOKEN_MISSING);
        }

        RefreshToken existing = refreshTokenService.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(BusinessErrorCodes.REFRESH_TOKEN_INVALID));

        Optional<RefreshToken> valid = refreshTokenService.verifyExpirationAndActive(existing);
        if (valid.isEmpty()) {
            throw new BusinessException(BusinessErrorCodes.REFRESH_TOKEN_INVALID);
        }

        refreshTokenService.revokeToken(existing);
        RefreshToken newRefresh = refreshTokenService.createRefreshToken(existing.getUser().getId());

        String newAccessToken = jwtTokenProvider.generateTokenFromUser(existing.getUser());

        Cookie cookie = new Cookie(refreshTokenCookieName, newRefresh.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(Math.toIntExact(refreshTokenCookieMaxAgeSeconds));
        cookie.setSecure(cookieSecure);
        response.addCookie(cookie);

        response.setHeader(HttpHeaders.SET_COOKIE,
                cookie.getName() + "=" + cookie.getValue()
                        + "; HttpOnly; Path=/; Max-Age=" + cookie.getMaxAge()
                        + "; SameSite=Strict"
                        + (cookieSecure ? "; Secure" : "")
        );

        return ResponseEntity.ok(new AuthResponse(newAccessToken));
    }

    // =========================
    // LOGOUT
    // =========================
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
        deleteCookie.setSecure(cookieSecure);
        response.addCookie(deleteCookie);

        response.setHeader(HttpHeaders.SET_COOKIE,
                deleteCookie.getName() + "=; HttpOnly; Path=/; Max-Age=0; SameSite=Strict"
                        + (cookieSecure ? "; Secure" : "")
        );

        return ResponseEntity.noContent().build();
    }
}