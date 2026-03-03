package com.mycompany.ecommerce.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.mycompany.ecommerce.handler.BusinessErrorCodes.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ====== BusinessException (your domain errors) ======
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ExceptionResponse> handleBusinessException(BusinessException ex, HttpServletRequest req) {
        return build(ex.getCode(), ex.getMessage(), null, req, null, null);
    }

    // ====== Security / Auth ======
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ExceptionResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        // Security best practice: do not reveal whether username exists
        return build(BAD_CREDENTIALS, "Nom d'utilisateur/email ou mot de passe incorrect.", null, req, null, null);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleUsernameNotFound(UsernameNotFoundException ex, HttpServletRequest req) {
        // Same response as bad credentials (avoid user enumeration)
        return build(BAD_CREDENTIALS, "Nom d'utilisateur/email ou mot de passe incorrect.", null, req, null, null);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ExceptionResponse> handleLocked(LockedException ex, HttpServletRequest req) {
        return build(ACCOUNT_LOCKED, "Votre compte est verrouillé.", ex.getMessage(), req, null, null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ExceptionResponse> handleDisabled(DisabledException ex, HttpServletRequest req) {
        return build(ACCOUNT_DISABLED, "Votre compte est désactivé.", ex.getMessage(), req, null, null);
    }

    // ====== Validation (@Valid) ======
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Set<String> validationErrors = new HashSet<>();
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String msg = error.getDefaultMessage();
            validationErrors.add(msg);

            if (error instanceof FieldError fe) {
                fieldErrors.put(fe.getField(), msg);
            }
        });

        return build(VALIDATION_ERROR, "Validation error.", null, req, validationErrors, fieldErrors);
    }

    // ====== Generic ======
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(BAD_REQUEST, ex.getMessage(), ex.getMessage(), req, null, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleException(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return build(INTERNAL_ERROR, "Erreur interne du serveur.", ex.getMessage(), req, null, null);
    }

    // ====== Helper ======
    private ResponseEntity<ExceptionResponse> build(
            BusinessErrorCodes code,
            String message,
            String technical,
            HttpServletRequest req,
            Set<String> validationErrors,
            Map<String, String> fieldErrors
    ) {
        ExceptionResponse body = ExceptionResponse.builder()
                .timestamp(Instant.now())
                .status(code.getHttpStatus().value())
                .path(req.getRequestURI())
                .businessErrorCode(code.getCode())
                .businessErrorDescription(code.getDescription())
                .message(message)
                .error(technical)
                .validationErrors(validationErrors)
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(code.getHttpStatus()).body(body);
    }
}