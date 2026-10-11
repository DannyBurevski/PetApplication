package com.burevski.msaccountreservation.exception;


import com.burevski.msaccountreservation.generated.dto.ErrorCode;
import com.burevski.msaccountreservation.generated.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClientNotFound(ClientNotFoundException exception) {
        log.info("{}", exception.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ErrorCode.CLIENT_NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParameter(MethodArgumentTypeMismatchException exception) {
        log.warn("Некорректный формат параметра: {}", exception.getName());
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST,
                "Некорректный формат параметра: " + exception.getName());
    }

    @ExceptionHandler(ClientAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleClientAlreadyExists(ClientAlreadyExistsException exception) {
        log.info("{}", exception.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ErrorCode.CLIENT_ALREADY_EXISTS, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String fields = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getField)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        log.warn("Невалидные поля запроса: {}", fields);
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST, "Невалидные поля: " + fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody() {
        log.warn("Не удалось прочитать тело запроса");
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST, "Некорректный JSON или типы полей");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        String description = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        log.warn("Ошибка валидации параметров запроса: {}", description);
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST, description);
    }

    @ExceptionHandler(ClientHasActiveAccountsException.class)
    public ResponseEntity<ErrorResponse> handleClientHasActiveAccounts(ClientHasActiveAccountsException exception) {
        log.info("{}", exception.getMessage());

        return buildResponse(HttpStatus.CONFLICT, ErrorCode.CLIENT_HAS_ACTIVE_ACCOUNTS, exception.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, ErrorCode errorCode, String description) {
        var response = new ErrorResponse();
        response.setErrorCode(errorCode);
        response.setErrorDescription(description);
        response.setStatusCode(status.value());
        return ResponseEntity.status(status).body(response);
    }
}

