package ru.practicum.main.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException e) {
        log.warn("404 Not Found: {}", e.getMessage());
        return buildApiError(HttpStatus.NOT_FOUND, "The required object was not found.", e.getMessage());
    }

    @ExceptionHandler({ConflictException.class, AlreadyExistsException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(RuntimeException e) {
        log.warn("409 Conflict: {}", e.getMessage());
        return buildApiError(HttpStatus.CONFLICT, "For the requested operation the conditions are not met.", e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("409 Data Integrity: {}", e.getMessage());
        return buildApiError(HttpStatus.CONFLICT, "Integrity constraint has been violated.", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException e) {
        log.warn("400 Validation Error: {}", e.getMessage());
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(err -> "Field: " + err.getField() + ". Error: " + err.getDefaultMessage() + ". Value: " + err.getRejectedValue())
                .collect(Collectors.toList());

        return ApiError.builder()
                .status(HttpStatus.BAD_REQUEST)
                .reason("Incorrectly made request.")
                .message("Validation failed")
                .errors(errors)
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(ConstraintViolationException e) {
        log.warn("400 Constraint Violation: {}", e.getMessage());
        return buildApiError(HttpStatus.BAD_REQUEST, "Incorrectly made request.", e.getMessage());
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            ValidationException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(Exception e) {
        log.warn("400 Bad Request: {}", e.getMessage());
        return buildApiError(HttpStatus.BAD_REQUEST, "Incorrectly made request.", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleInternalError(Exception e) {
        log.error("500 Internal Server Error", e);
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .reason("Internal server error.")
                .message(e.getMessage() != null ? e.getMessage() : "Unknown error")
                .errors(List.of(sw.toString()))
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
    }

    private ApiError buildApiError(HttpStatus status, String reason, String message) {
        return ApiError.builder()
                .status(status)
                .reason(reason)
                .message(message)
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .errors(Collections.emptyList())
                .build();
    }
}