package com.mycompany.ecommerce.auth;

import com.mycompany.ecommerce.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "verification_codes", indexes = {
        @Index(columnList = "code", name = "idx_verif_code"),
        @Index(columnList = "expiresAt", name = "idx_verif_expires")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 6 digits
    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;
}