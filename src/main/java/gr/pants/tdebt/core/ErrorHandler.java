package gr.pants.tdebt.core;

import gr.pants.tdebt.core.exceptions.*;
import gr.pants.tdebt.dto.ErrorResponseDTO;
import gr.pants.tdebt.dto.ValidationErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    private final MessageSource messageSource;

    public ErrorHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponseDTO> handleValidation(ValidationException e) {
        log.warn("Validation failed. Message={}", e.getMessage());

        BindingResult bindingResult = e.getBindingResult();

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            String message = messageSource.getMessage(
                    fieldError.getCodes()[0],
                    fieldError.getArguments(),
                    fieldError.getDefaultMessage(),
                    Locale.ENGLISH
            );
            errors.put(fieldError.getField(), message);
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponseDTO(
                        e.getErrorCode(),
                        e.getMessage(),
                        errors
                ));
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleAlreadyExists(EntityAlreadyExistsException e) {
        log.warn("Entity already exists. Message={}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDTO(
                        e.getErrorCode(),
                        e.getMessage()
                ));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(EntityNotFoundException e) {
        log.warn("Entity not found. Message={}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDTO(
                        e.getErrorCode(),
                        e.getMessage()
                ));
    }

    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidArgument(InvalidArgumentException e) {
        log.warn("Invalid argument. Message={}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(
                        e.getErrorCode(),
                        e.getMessage()
                ));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponseDTO> handleInsufficientBalance(InsufficientBalanceException e) {
        log.warn("Insufficient balance. message={}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(new ErrorResponseDTO(
                        e.getErrorCode(),
                        e.getMessage()
                ));
    }

    @ExceptionHandler(DebtHasTransactionsException.class)
    public ResponseEntity<ErrorResponseDTO> handleHasTransactions(DebtHasTransactionsException e) {
        log.warn("Cannot delete Debt with existing transactions. message={}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDTO(
                        e.getErrorCode(),
                        e.getMessage()
                ));
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handlePasswordMismatch(PasswordMismatchException e) {
        log.warn("Password mismatch. message={}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(new ErrorResponseDTO(
                        e.getErrorCode(),
                        e.getMessage()
                ));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnauthorized(UnauthorizedException e, WebRequest request) {
        log.warn("Authorization failed for URI={}. Message={}", request.getDescription(false), e.getMessage()); // uri=/api/user/...
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponseDTO(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponseDTO> handleDatabaseError(DataAccessException e) {
        log.warn("Database error. Message={}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO(
                        "DATABASE_ERROR",
                        "A database error occurred"
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthentication(AuthenticationException e,
                                                                 HttpServletRequest request) {
        log.warn("Failed login for IP={}", request.getRemoteAddr());

        String errorCode = switch (e) {
            case BadCredentialsException ex -> "INVALID_CREDENTIALS";
            case DisabledException ex -> "ACCOUNT_DISABLED";
            case LockedException ex -> "ACCOUNT_LOCKED";
            case AccountExpiredException ex -> "ACCOUNT_EXPIRED";
            case CredentialsExpiredException ex -> "CREDENTIALS_EXPIRED";
            default -> "AUTHENTICATION_ERROR";
        };

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponseDTO(errorCode, e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)       // 403 Forbidden
                .body(new ErrorResponseDTO("ACCESS_DENIED", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleUnexpectedError(Exception e) {
        log.warn("Unexpected error. Message={}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO(
                        "INTERNAL_SERVER_ERROR",
                        "Unexpected error occurred"
                ));
    }
}
