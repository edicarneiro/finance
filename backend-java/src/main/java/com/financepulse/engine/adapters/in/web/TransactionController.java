package com.financepulse.engine.adapters.in.web;

import com.financepulse.engine.adapters.in.web.dto.CreateTransactionRequest;
import com.financepulse.engine.adapters.in.web.dto.CreateTransactionResponse;
import com.financepulse.engine.adapters.in.web.dto.TransactionResponse;
import com.financepulse.engine.adapters.in.web.dto.UpdateTransactionRequest;
import com.financepulse.engine.application.usecases.transaction.CreateTransactionUseCase;
import com.financepulse.engine.application.usecases.transaction.DeleteTransactionUseCase;
import com.financepulse.engine.application.usecases.transaction.ListTransactionsUseCase;
import com.financepulse.engine.application.usecases.transaction.UpdateTransactionUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas protegidas por {@link AuthenticationInterceptor} (/transactions/**).
 * Listagem é somente por conta nesta fase — filtro/busca completo (RF-018) é
 * a Fase 4.3 (ver ADR-0016).
 */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final CreateTransactionUseCase createTransactionUseCase;
    private final UpdateTransactionUseCase updateTransactionUseCase;
    private final DeleteTransactionUseCase deleteTransactionUseCase;
    private final ListTransactionsUseCase listTransactionsUseCase;

    public TransactionController(
            CreateTransactionUseCase createTransactionUseCase,
            UpdateTransactionUseCase updateTransactionUseCase,
            DeleteTransactionUseCase deleteTransactionUseCase,
            ListTransactionsUseCase listTransactionsUseCase) {
        this.createTransactionUseCase = createTransactionUseCase;
        this.updateTransactionUseCase = updateTransactionUseCase;
        this.deleteTransactionUseCase = deleteTransactionUseCase;
        this.listTransactionsUseCase = listTransactionsUseCase;
    }

    @PostMapping
    public ResponseEntity<CreateTransactionResponse> create(
            @Valid @RequestBody CreateTransactionRequest request, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        CreateTransactionUseCase.Output output = createTransactionUseCase.execute(new CreateTransactionUseCase.Input(
                userId,
                request.accountId(),
                request.categoryId(),
                request.type(),
                request.amount(),
                request.date(),
                request.description(),
                request.tags()));

        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateTransactionResponse(output.transactionId()));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> list(@RequestParam String accountId, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        ListTransactionsUseCase.Output output = listTransactionsUseCase.execute(new ListTransactionsUseCase.Input(userId, accountId));

        return ResponseEntity.ok(output.transactions().stream().map(TransactionResponse::from).toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable String id, @Valid @RequestBody UpdateTransactionRequest request, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        updateTransactionUseCase.execute(new UpdateTransactionUseCase.Input(
                userId,
                id,
                request.accountId(),
                request.categoryId(),
                request.type(),
                request.amount(),
                request.date(),
                request.description(),
                request.tags()));

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        deleteTransactionUseCase.execute(new DeleteTransactionUseCase.Input(userId, id));

        return ResponseEntity.noContent().build();
    }
}
