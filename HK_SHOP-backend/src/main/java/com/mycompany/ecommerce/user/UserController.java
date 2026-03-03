package com.mycompany.ecommerce.user;

import com.mycompany.ecommerce.user.dto.ChangePasswordRequest;
import com.mycompany.ecommerce.user.dto.ProfileResponse;
import com.mycompany.ecommerce.user.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ProfileResponse> me(Authentication authentication) {
        return ResponseEntity.ok(userService.getMyProfile(authentication));
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> update(
            @Valid @RequestBody UpdateProfileRequest req,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userService.updateMyProfile(req, authentication));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            Authentication authentication
    ) {
        userService.changeMyPassword(req, authentication);
        return ResponseEntity.noContent().build();
    }
}