package com.truckpulse.telemetry.exception;

import com.truckpulse.telemetry.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler   {
    @ExceptionHandler(TruckNotFoundException.class)
    public ResponseEntity<ApiError> handleTruckNotFound(
            TruckNotFoundException exception) {

        ApiError error = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

}
