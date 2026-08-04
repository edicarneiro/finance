package com.financepulse.engine.adapters.in.web;

import com.financepulse.engine.adapters.in.web.dto.ErrorResponse;
import com.financepulse.engine.domain.account.errors.AccountNotFoundException;
import com.financepulse.engine.domain.account.errors.ArchivedAccountException;
import com.financepulse.engine.domain.account.errors.InvalidAccountNameException;
import com.financepulse.engine.domain.account.errors.InvalidCurrencyException;
import com.financepulse.engine.domain.backoffice.errors.ForbiddenException;
import com.financepulse.engine.domain.budget.errors.BudgetNotFoundException;
import com.financepulse.engine.domain.budget.errors.InvalidAlertThresholdException;
import com.financepulse.engine.domain.budget.errors.InvalidBudgetLimitException;
import com.financepulse.engine.domain.budget.errors.InvalidBudgetPeriodException;
import com.financepulse.engine.domain.category.errors.CategoryHasSubcategoriesException;
import com.financepulse.engine.domain.category.errors.CategoryHasTransactionsException;
import com.financepulse.engine.domain.category.errors.CategoryNotFoundException;
import com.financepulse.engine.domain.category.errors.InvalidCategoryHierarchyException;
import com.financepulse.engine.domain.category.errors.InvalidCategoryNameException;
import com.financepulse.engine.domain.goal.errors.GoalNotFoundException;
import com.financepulse.engine.domain.goal.errors.InvalidGoalAssociationException;
import com.financepulse.engine.domain.goal.errors.InvalidGoalDeadlineException;
import com.financepulse.engine.domain.goal.errors.InvalidGoalNameException;
import com.financepulse.engine.domain.goal.errors.InvalidGoalTargetException;
import com.financepulse.engine.domain.goal.errors.InvalidGoalThresholdException;
import com.financepulse.engine.domain.notification.errors.NotificationNotFoundException;
import com.financepulse.engine.domain.report.errors.InvalidReportPeriodException;
import com.financepulse.engine.domain.transaction.errors.InvalidAmountException;
import com.financepulse.engine.domain.transaction.errors.TransactionNotFoundException;
import com.financepulse.engine.domain.user.errors.AccountSuspendedException;
import com.financepulse.engine.domain.user.errors.DuplicateEmailException;
import com.financepulse.engine.domain.user.errors.InvalidConsentVersionException;
import com.financepulse.engine.domain.user.errors.InvalidCredentialsException;
import com.financepulse.engine.domain.user.errors.InvalidEmailException;
import com.financepulse.engine.domain.user.errors.UserNotFoundException;
import com.financepulse.engine.domain.user.errors.WeakPasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
        InvalidCurrencyException.class,
        InvalidCategoryNameException.class,
        InvalidAmountException.class,
        ArchivedAccountException.class,
        InvalidCategoryHierarchyException.class,
        CategoryHasSubcategoriesException.class,
        CategoryHasTransactionsException.class,
        InvalidBudgetLimitException.class,
        InvalidAlertThresholdException.class,
        InvalidBudgetPeriodException.class,
        InvalidGoalNameException.class,
        InvalidGoalTargetException.class,
        InvalidGoalDeadlineException.class,
        InvalidGoalAssociationException.class,
        InvalidGoalThresholdException.class,
        InvalidReportPeriodException.class,
        InvalidConsentVersionException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException error) {
        return ResponseEntity.badRequest().body(new ErrorResponse(error.getMessage()));
    }

    @ExceptionHandler({ForbiddenException.class, AccountSuspendedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException error) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(error.getMessage()));
    }

    @ExceptionHandler({
        AccountNotFoundException.class,
        CategoryNotFoundException.class,
        TransactionNotFoundException.class,
        BudgetNotFoundException.class,
        GoalNotFoundException.class,
        NotificationNotFoundException.class,
        UserNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(error.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException error) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Corpo da requisição inválido ou incompleto."));
    }

    // Uma rota inexistente (ex.: um navegador pedindo "/" ou "/favicon.ico") é um 404 comum de
    // cliente, não um erro de aplicação — não deve cair no handler genérico de 500 nem ser logada
    // como ERROR (rules.md §5: erro real vs. comportamento esperado de cliente).
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Recurso não encontrado."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception error) {
        logger.error("Unhandled error", error);
        return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno inesperado."));
    }
}
