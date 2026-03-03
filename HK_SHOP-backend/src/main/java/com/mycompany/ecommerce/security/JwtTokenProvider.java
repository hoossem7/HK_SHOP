package com.mycompany.ecommerce.security;

import com.mycompany.ecommerce.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key key = Keys.hmacShaKeyFor(
            "replace-with-very-long-secret-key-should-be-256-bit-minimum"
                    .getBytes(StandardCharsets.UTF_8)
    );

    private final long validityMs = 1000L * 60 * 60 * 24; // 24h

    // subject = userId
    public String generateTokenFromUser(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMs);

        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractSubject(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();
            Object u = claims.get("username");
            return u != null ? String.valueOf(u) : null;
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    // ✅ validation simple: expiration + signature
    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}