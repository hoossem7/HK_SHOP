package com.mycompany.ecommerce.user;

import com.mycompany.ecommerce.handler.BusinessErrorCodes;
import com.mycompany.ecommerce.handler.BusinessException;
import com.mycompany.ecommerce.user.dto.ChangePasswordRequest;
import com.mycompany.ecommerce.user.dto.ProfileResponse;
import com.mycompany.ecommerce.user.dto.UpdateProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileResponse getMyProfile(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return mapToProfile(user);
    }

    @Transactional
    public ProfileResponse updateMyProfile(UpdateProfileRequest req, Authentication authentication) {
        User user = getCurrentUser(authentication);

        if (!user.getUsername().equals(req.username()) && userRepository.existsByUsername(req.username())) {
            throw new BusinessException(BusinessErrorCodes.USERNAME_TAKEN);
        }

        if (!user.getEmail().equals(req.email()) && userRepository.existsByEmail(req.email())) {
            throw new BusinessException(BusinessErrorCodes.EMAIL_TAKEN);
        }

        user.setUsername(req.username());
        user.setEmail(req.email());
        userRepository.save(user);

        return mapToProfile(user);
    }

    @Transactional
    public void changeMyPassword(ChangePasswordRequest req, Authentication authentication) {
        User user = getCurrentUser(authentication);

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new BusinessException(BusinessErrorCodes.INCORRECT_CURRENT_PASSWORD);
        }

        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new BusinessException(BusinessErrorCodes.NEW_PASSWORD_DOES_NOT_MATCH);
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    // ====================== helpers ======================

    private User getCurrentUser(Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(BusinessErrorCodes.UNAUTHORIZED_ACCESS);
        }

        Long userId;
        Object principal = authentication.getPrincipal();

        if (principal instanceof Long l) {
            userId = l;
        } else if (principal instanceof String s) {
            try {
                userId = Long.parseLong(s);
            } catch (NumberFormatException e) {
                throw new BusinessException(BusinessErrorCodes.UNAUTHORIZED_ACCESS);
            }
        } else {
            throw new BusinessException(BusinessErrorCodes.UNAUTHORIZED_ACCESS);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCodes.USER_NOT_FOUND));
    }

    private ProfileResponse mapToProfile(User user) {
        return new ProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRoles());
    }
}