package ru.practicum.ewm.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        ApiError err = ApiError.builder()
                               .status(HttpStatus.NOT_FOUND.getReasonPhrase())
                               .reason("The required object was not found.")
                               .message(ex.getMessage())
                               .timestamp(LocalDateTime.now())
                               .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }

    @ExceptionHandler({
            BadRequestException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            ValidationException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        ApiError err = ApiError.builder()
                               .status(HttpStatus.BAD_REQUEST.getReasonPhrase())
                               .reason("Incorrectly made request.")
                               .message(ex.getMessage())
                               .timestamp(LocalDateTime.now())
                               .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        ApiError err = ApiError.builder()
                               .status(HttpStatus.CONFLICT.getReasonPhrase())
                               .reason("Integrity constraint has been violated.")
                               .message(ex.getMessage())
                               .timestamp(LocalDateTime.now())
                               .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        ApiError err = ApiError.builder()
                               .status(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                               .reason("Unexpected error")
                               .message(ex.getMessage())
                               .timestamp(LocalDateTime.now())
                               .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
}



