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
    RESOURCE_ALREADY_EXISTS(1006, "Resource already exists", HttpStatus.CONFLICT),
    TABLE_LOCKED(1007, "Bàn đang bị khóa tạm thời hoặc bị khóa gọi món", HttpStatus.FORBIDDEN),
    DEVICE_LIMIT_EXCEEDED(1008, "Bàn đã đạt giới hạn thiết bị truy cập đồng thời", HttpStatus.TOO_MANY_REQUESTS),
    HOST_PERMISSION_REQUIRED(1009, "Chỉ Chủ Bàn mới có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    DEVICE_NOT_FOUND(1010, "Không tìm thấy thiết bị yêu cầu", HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
