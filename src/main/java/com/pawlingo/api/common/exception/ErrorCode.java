package com.pawlingo.api.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "Email already registered"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Invalid request"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Missing or invalid access token"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong"),
    GOOGLE_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Invalid Google ID token"),
    GOOGLE_EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Google account email is not verified"),
    ACCOUNT_EXISTS_WITH_PASSWORD(HttpStatus.CONFLICT, "An account with this email already exists, log in with your password"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"),
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "Pet not found for this user");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return name();
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
