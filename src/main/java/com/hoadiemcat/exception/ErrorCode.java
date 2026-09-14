package com.hoadiemcat.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(1002, "Resource not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1003, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1004, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_REQUEST(1005, "Invalid request parameter", HttpStatus.BAD_REQUEST),
    RESOURCE_ALREADY_EXISTS(1006, "Resource already exists", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
