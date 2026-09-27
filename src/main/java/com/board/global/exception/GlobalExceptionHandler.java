package com.board.global.exception;

import com.board.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(
                        ApiResponse.fail(
                                e.getErrorCode().name(),
                                e.getErrorCode().getMessage()
                        )
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getStatus())
                .body(
                        ApiResponse.fail(
                                ErrorCode.INVALID_INPUT.name(),
                                ErrorCode.INVALID_INPUT.getMessage(),
                                e.getBindingResult().getFieldErrors()
                                        .stream()
                                        .map(fieldError -> new ApiResponse.ValidationError(
                                                fieldError.getField(),
                                                fieldError.getDefaultMessage()
                                        ))
                                        .toList()
                        )
                );
    }
}
