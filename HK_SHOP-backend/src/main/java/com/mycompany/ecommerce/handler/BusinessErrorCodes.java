package com.mycompany.ecommerce.handler;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BusinessErrorCodes {

    // Generic
    NO_CODE(0, HttpStatus.INTERNAL_SERVER_ERROR, "No code"),
    INTERNAL_ERROR(1, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),

    // Validation
    VALIDATION_ERROR(100, HttpStatus.BAD_REQUEST, "Validation error"),
    BAD_REQUEST(101, HttpStatus.BAD_REQUEST, "Bad request"),
    RESOURCE_NOT_FOUND(102, HttpStatus.NOT_FOUND, "Resource not found"),

    // Auth
    BAD_CREDENTIALS(304, HttpStatus.UNAUTHORIZED, "Login and/or password is incorrect"),
    ACCOUNT_LOCKED(302, HttpStatus.FORBIDDEN, "User account is locked"),
    ACCOUNT_DISABLED(303, HttpStatus.FORBIDDEN, "User account is disabled"),
    USER_NOT_FOUND(307, HttpStatus.NOT_FOUND, "User not found"),

    // Register
    USERNAME_TAKEN(305, HttpStatus.BAD_REQUEST, "Username is already taken"),
    EMAIL_TAKEN(306, HttpStatus.BAD_REQUEST, "Email is already in use"),

    // Refresh
    REFRESH_TOKEN_MISSING(40101, HttpStatus.UNAUTHORIZED, "Refresh token is missing"),
    REFRESH_TOKEN_INVALID(40102, HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired"),

    // Token / Activation
    TOKEN_INVALID(310, HttpStatus.BAD_REQUEST, "Token is invalid"),
    TOKEN_EXPIRED(311, HttpStatus.BAD_REQUEST, "Token is expired or already used"),

    // Activation
    ACTIVATION_CODE_INVALID(310, HttpStatus.BAD_REQUEST, "Activation code is invalid"),
    ACTIVATION_CODE_EXPIRED(311, HttpStatus.BAD_REQUEST, "Activation code is expired"),
    ACTIVATION_CODE_USED(312, HttpStatus.BAD_REQUEST, "Activation code already used"),

    // Authorization
    UNAUTHORIZED_ACCESS(401, HttpStatus.UNAUTHORIZED, "Unauthorized access"),
    FORBIDDEN_ACCESS(403, HttpStatus.FORBIDDEN, "Access denied"),

    // Password
    INCORRECT_CURRENT_PASSWORD(308, HttpStatus.BAD_REQUEST, "Current password is incorrect"),
    NEW_PASSWORD_DOES_NOT_MATCH(309, HttpStatus.BAD_REQUEST, "New password does not match confirmation");

    private final int code;
    private final HttpStatus httpStatus;
    private final String description;

    BusinessErrorCodes(int code, HttpStatus httpStatus, String description) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.description = description;
    }
}