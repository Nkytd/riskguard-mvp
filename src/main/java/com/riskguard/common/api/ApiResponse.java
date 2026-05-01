package com.riskguard.common.api;

import com.riskguard.common.trace.TraceIdHolder;

import java.time.LocalDateTime;

public record ApiResponse<T>(
        int code,
        String message,
        T data,
        String traceId,
        LocalDateTime timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, TraceIdHolder.getTraceId(), LocalDateTime.now());
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode.code(), errorCode.message());
    }

    public static ApiResponse<Void> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, TraceIdHolder.getTraceId(), LocalDateTime.now());
    }
}
