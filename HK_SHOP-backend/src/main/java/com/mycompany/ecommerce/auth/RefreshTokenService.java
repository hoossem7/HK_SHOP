package com.mycompany.ecommerce.auth;

import com.mycompany.ecommerce.handler.BusinessErrorCodes;
import com.mycompany.ecommerce.handler.BusinessException;
import com.mycompany.ecommerce.user.User;
import com.mycompany.ecommerce.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.refreshTokenExpirationMs:2592000000}")
    private Long refreshTokenDurationMs;

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCodes.USER_NOT_FOUND));

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .token(UUID.randomUUID().toString())
                .revoked(false)
                .build();

        return refreshTokenRepository.save(token);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    // ✅ AJOUTÉE pour correspondre à ton AuthController
    @Transactional
    public Optional<RefreshToken> verifyExpirationAndActive(RefreshToken token) {
        if (token.isRevoked() || token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            return Optional.empty();
        }
        return Optional.of(token);
    }

    // (tu la gardes si tu veux l'utiliser plus tard ailleurs)
    @Transactional
    public void deleteIfExpiredOrRevoked(RefreshToken token) {
        if (token.isRevoked() || token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new BusinessException(BusinessErrorCodes.REFRESH_TOKEN_INVALID);
        }
    }

    @Transactional
    public void revokeToken(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void revokeAllTokensForUser(Long userId) {
        userRepository.findById(userId).ifPresent(refreshTokenRepository::deleteByUser);
    }
}