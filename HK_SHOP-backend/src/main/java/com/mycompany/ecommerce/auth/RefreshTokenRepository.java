package com.mycompany.ecommerce.auth;

import com.mycompany.ecommerce.auth.RefreshToken;
import com.mycompany.ecommerce.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
}
