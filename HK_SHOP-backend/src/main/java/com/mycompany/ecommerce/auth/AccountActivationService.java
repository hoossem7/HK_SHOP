package com.mycompany.ecommerce.auth;

import com.mycompany.ecommerce.handler.BusinessErrorCodes;
import com.mycompany.ecommerce.handler.BusinessException;
import com.mycompany.ecommerce.mail.EmailService;
import com.mycompany.ecommerce.mail.EmailTemplateService;
import com.mycompany.ecommerce.user.User;
import com.mycompany.ecommerce.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AccountActivationService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;

    // ✅ AJOUT: services email
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;

    @Value("${app.mail.verificationCodeExpirationMinutes:10}")
    private long expirationMinutes;

    private final SecureRandom random = new SecureRandom();

    /**
     * ✅ Utilisée par le controller:
     * - quand user est nouveau
     * - quand user existe déjà mais enabled=false (renvoi code)
     *
     * Stratégie: rotation => on supprime anciens codes et on crée un nouveau
     */
    @Transactional
    public VerificationCode resendCodeFor(User user) {

        // (optionnel) si déjà activé, ne rien faire
        if (user.isEnabled()) {
            return null; // ou throw si tu préfères: ACCOUNT_ALREADY_ACTIVE
        }

        // rotation: supprimer tous les anciens codes de ce user
        verificationCodeRepository.deleteByUser_Id(user.getId());

        // créer nouveau
        String code = generate6Digits();
        VerificationCode vc = VerificationCode.builder()
                .user(user)
                .code(code)
                .used(false)
                .expiresAt(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES))
                .build();

        VerificationCode saved = verificationCodeRepository.save(vc);

        // ✅ ENVOI EMAIL (ce qui manquait)
        String subject = "HK Shop - Code d’activation";
        String content = emailTemplateService.buildActivationCodeEmail(
                user.getUsername(),
                code,
                expirationMinutes
        );

        emailService.sendEmail(user.getEmail(), subject, content);

        return saved;
    }

    /**
     * Gardée si tu veux l’utiliser ailleurs, sinon tu peux supprimer et n’utiliser que resendCodeFor()
     */
    @Transactional
    public VerificationCode createCodeFor(User user) {
        return resendCodeFor(user);
    }

    /** Active compte via email + code */
    @Transactional
    public void activateByCode(String email, String code) {

        VerificationCode vc = verificationCodeRepository
                .findTopByUser_EmailAndCodeOrderByIdDesc(email, code)
                .orElseThrow(() -> new BusinessException(BusinessErrorCodes.ACTIVATION_CODE_INVALID));

        if (vc.isUsed()) {
            throw new BusinessException(BusinessErrorCodes.ACTIVATION_CODE_USED);
        }

        if (vc.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(BusinessErrorCodes.ACTIVATION_CODE_EXPIRED);
        }

        User user = vc.getUser();

        // si déjà activé => on peut considérer OK (idempotent) ou throw
        if (!user.isEnabled()) {
            user.setEnabled(true);
            userRepository.save(user);
        }

        vc.setUsed(true);
        verificationCodeRepository.save(vc);
    }

    private String generate6Digits() {
        int n = 100000 + random.nextInt(900000);
        return String.valueOf(n);
    }
}