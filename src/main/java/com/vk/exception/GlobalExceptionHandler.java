package com.vk.exception;

import com.vk.records.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> productNotFoundException(ProductNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.failure(null, e.getMessage())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> internalServerError(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.failure(null, e.getMessage())
        );
    }

}
