package com.truckpulse.telemetry.exception;

import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler   {
    @ExceptionHandler
    public ResponseEntity<String> handleTruckNotFoundException(TruckNotFoundException exception) {
        return ResponseEntity
                .notFound()
                .build();
    }
}
