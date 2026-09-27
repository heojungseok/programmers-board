package com.board.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@Getter
public class ApiResponse<T> {
    private final String code;
    private final String message;
    private final T data;
    private final List<ValidationError> errors;

    public ApiResponse(String code, String message, T data, List<ValidationError> errors) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.errors = errors;
    }

    public ApiResponse(String code, String message, T data) {
        this(code, message, data, null);
    }

    public ApiResponse(String code, String message) {
        this(code, message, null);
    }

    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(code, message);
    }

    public static <T> ApiResponse<T> fail(String code, String message, List<ValidationError> errors) {
        return new ApiResponse<>(code, message, null, errors);
    }

    @Getter
    @RequiredArgsConstructor
    public static class ValidationError {
        private final String field;
        private final String reason;
    }
}

