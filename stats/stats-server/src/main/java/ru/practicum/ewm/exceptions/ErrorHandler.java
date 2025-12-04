package ru.practicum.ewm.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.format.DateTimeParseException;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    /**
     * 400 — некорректные данные в параметрах запроса.
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,         // ошибки @Valid
            MethodArgumentTypeMismatchException.class,     // неверный тип параметра (?unique=abc)
            MissingServletRequestParameterException.class, // отсутствует обязательный параметр
            DateTimeParseException.class,                  // неверный формат даты
            HttpMessageNotReadableException.class          // плохое тело JSON
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handle400(Exception e) {
        log.warn("400 BAD REQUEST: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    /**
     * 400 — наше приложение выбросило BadRequestException.
     * Spring сам раскроет даже вложенные исключения.
     */
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequestException(BadRequestException e) {
        log.warn("400 BAD REQUEST (custom): {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    /**
     * 500 — все остальные неперехваченные исключения.
     */
    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handle500(Exception e) {
        log.error("500 INTERNAL SERVER ERROR: {}", e.getMessage(), e);

        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));

        return new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                e.getMessage(),
                sw.toString()
        );
    }
}