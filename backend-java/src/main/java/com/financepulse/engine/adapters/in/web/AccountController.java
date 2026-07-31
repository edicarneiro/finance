package com.financepulse.engine.adapters.in.web;

import com.financepulse.engine.adapters.in.web.dto.AccountResponse;
import com.financepulse.engine.adapters.in.web.dto.ConsolidatedBalanceResponse;
import com.financepulse.engine.adapters.in.web.dto.CreateAccountRequest;
import com.financepulse.engine.adapters.in.web.dto.CreateAccountResponse;
import com.financepulse.engine.adapters.in.web.dto.UpdateAccountRequest;
import com.financepulse.engine.application.usecases.account.ArchiveAccountUseCase;
import com.financepulse.engine.application.usecases.account.CreateAccountUseCase;
import com.financepulse.engine.application.usecases.account.GetConsolidatedBalanceUseCase;
import com.financepulse.engine.application.usecases.account.ListAccountsUseCase;
import com.financepulse.engine.application.usecases.account.UpdateAccountUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rotas protegidas por {@link AuthenticationInterceptor} (registrado apenas para /accounts/**, ver WebMvcConfig). */
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final UpdateAccountUseCase updateAccountUseCase;
    private final ArchiveAccountUseCase archiveAccountUseCase;
    private final ListAccountsUseCase listAccountsUseCase;
    private final GetConsolidatedBalanceUseCase getConsolidatedBalanceUseCase;

    public AccountController(
            CreateAccountUseCase createAccountUseCase,
            UpdateAccountUseCase updateAccountUseCase,
            ArchiveAccountUseCase archiveAccountUseCase,
            ListAccountsUseCase listAccountsUseCase,
            GetConsolidatedBalanceUseCase getConsolidatedBalanceUseCase) {
        this.createAccountUseCase = createAccountUseCase;
        this.updateAccountUseCase = updateAccountUseCase;
        this.archiveAccountUseCase = archiveAccountUseCase;
        this.listAccountsUseCase = listAccountsUseCase;
        this.getConsolidatedBalanceUseCase = getConsolidatedBalanceUseCase;
    }

    @PostMapping
    public ResponseEntity<CreateAccountResponse> create(@Valid @RequestBody CreateAccountRequest request, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        CreateAccountUseCase.Output output = createAccountUseCase.execute(new CreateAccountUseCase.Input(
                userId, request.type(), request.name(), request.currency(), request.initialBalance()));

        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateAccountResponse(output.accountId()));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> list(HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        ListAccountsUseCase.Output output = listAccountsUseCase.execute(new ListAccountsUseCase.Input(userId));

        return ResponseEntity.ok(output.accounts().stream().map(AccountResponse::from).toList());
    }

    @GetMapping("/balance/consolidated")
    public ResponseEntity<ConsolidatedBalanceResponse> consolidatedBalance(HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        GetConsolidatedBalanceUseCase.Output output =
                getConsolidatedBalanceUseCase.execute(new GetConsolidatedBalanceUseCase.Input(userId));

        return ResponseEntity.ok(new ConsolidatedBalanceResponse(output.consolidatedBalance()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable String id, @Valid @RequestBody UpdateAccountRequest request, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        updateAccountUseCase.execute(new UpdateAccountUseCase.Input(userId, id, request.name()));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable String id, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        archiveAccountUseCase.execute(new ArchiveAccountUseCase.Input(userId, id));

        return ResponseEntity.noContent().build();
    }
}
