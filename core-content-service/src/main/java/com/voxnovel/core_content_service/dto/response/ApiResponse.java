package com.voxnovel.core_content_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.voxnovel.core_content_service.exception.ErrorCode;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    @Builder.Default
    int code = ErrorCode.SUCCESS.getCode();
    String message;
    T result;

    public static <T> ApiResponse<T> success(T result) {
        return ApiResponse.<T>builder()
                .code(ErrorCode.SUCCESS.getCode())
                .result(result)
                .build();
    }

    public static <T> ApiResponse<T> success(T result, String message) {
        return ApiResponse.<T>builder()
                .code(ErrorCode.SUCCESS.getCode())
                .message(message)
                .result(result)
                .build();
    }

    public static ApiResponse<Void> successMessage(String message) {
        return ApiResponse.<Void>builder()
                .code(ErrorCode.SUCCESS.getCode())
                .message(message)
                .build();
    }

    public static ApiResponse<Void> accepted(String message) {
        return ApiResponse.<Void>builder()
                .code(ErrorCode.ACCEPTED.getCode())
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return ApiResponse.<T>builder()
                .code(errorCode.getCode())
                .message(message)
                .build();
    }
}
