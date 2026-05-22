package com.voxnovel.core_content_service.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(1000, "Thành công"),
    ACCEPTED(1001, "Đã tiếp nhận yêu cầu"),
    NOT_FOUND(1002, "Không tìm thấy tài nguyên"),
    BAD_REQUEST(1003, "Yêu cầu không hợp lệ"),
    UNCATEGORIZED_EXCEPTION(9999, "Đã xảy ra lỗi hệ thống");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
