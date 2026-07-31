package com.financepulse.engine.adapters.in.web;

import com.financepulse.engine.adapters.in.web.dto.ErrorResponse;
import com.financepulse.engine.domain.account.errors.AccountNotFoundException;
import com.financepulse.engine.domain.account.errors.InvalidAccountNameException;
import com.financepulse.engine.domain.account.errors.InvalidCurrencyException;
import com.financepulse.engine.domain.user.errors.DuplicateEmailException;
import com.financepulse.engine.domain.user.errors.InvalidCredentialsException;
import com.financepulse.engine.domain.user.errors.InvalidEmailException;
import com.financepulse.engine.domain.user.errors.WeakPasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Mapeia erros de domínio para respostas HTTP sem vazar detalhes internos
 * (stack traces, mensagens de driver) ao cliente, espelhando o
 * errorHandler.ts do backend TypeScript (rules.md §4, Segurança).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException error) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(error.getMessage()));
    }

    @ExceptionHandler({
        DuplicateEmailException.class,
        InvalidEmailException.class,
        WeakPasswordException.class,
        InvalidAccountNameException.class,
        InvalidCurrencyException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException error) {
        return ResponseEntity.badRequest().body(new ErrorResponse(error.getMessage()));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(error.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException error) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Corpo da requisição inválido ou incompleto."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception error) {
        logger.error("Unhandled error", error);
        return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno inesperado."));
    }
}
