package com.voxnovel.identity_service.exception;

import lombok.Getter;

import javax.tools.Diagnostic;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Unknown error"),
    USER_EXISTED(1001, "User Exist"),
    USER_NOT_FOUND(1002, "User not found"),
    INVALID_KEY(1003, "Invalid Message Key");

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    private int code;
    private String message;
}
