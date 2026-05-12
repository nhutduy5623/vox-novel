package com.voxnovel.identity_service.exception;

import com.voxnovel.identity_service.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    // 1. Bắt tất cả các lỗi chưa được khai báo (Lỗi hệ thống, NullPointer, v.v.)
    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<Object>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse<Object> apiResponse = ApiResponse.<Object>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                // .result(null) -> Thằng Jackson sẽ tự hiểu và loại bỏ nhờ @JsonInclude(NON_NULL)
                .build();

        return ResponseEntity.badRequest().body(apiResponse);
    }

    // Bắt các lỗi hệ thống chưa xác định
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<Object>> handlingRuntimeException(Exception exception) {
        log.error("Error system: ", exception);
        ApiResponse<Object> apiResponse = ApiResponse.<Object>builder()
                .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }

}
