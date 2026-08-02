package com.financepulse.engine.adapters.in.web;

import com.financepulse.engine.adapters.in.web.dto.BudgetPeriodPerformanceResponse;
import com.financepulse.engine.adapters.in.web.dto.BudgetResponse;
import com.financepulse.engine.adapters.in.web.dto.CreateBudgetRequest;
import com.financepulse.engine.adapters.in.web.dto.CreateBudgetResponse;
import com.financepulse.engine.adapters.in.web.dto.UpdateBudgetRequest;
import com.financepulse.engine.application.usecases.budget.CreateBudgetUseCase;
import com.financepulse.engine.application.usecases.budget.DeleteBudgetUseCase;
import com.financepulse.engine.application.usecases.budget.GetBudgetHistoryUseCase;
import com.financepulse.engine.application.usecases.budget.ListBudgetsUseCase;
import com.financepulse.engine.application.usecases.budget.UpdateBudgetUseCase;
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

/** Rotas protegidas por {@link AuthenticationInterceptor}. RF-026, RF-027, RF-029; RF-028 é apenas o sinal (ver ADR-0018). */
@RestController
@RequestMapping("/budgets")
public class BudgetController {

    private final CreateBudgetUseCase createBudgetUseCase;
    private final UpdateBudgetUseCase updateBudgetUseCase;
    private final DeleteBudgetUseCase deleteBudgetUseCase;
    private final ListBudgetsUseCase listBudgetsUseCase;
    private final GetBudgetHistoryUseCase getBudgetHistoryUseCase;

    public BudgetController(
            CreateBudgetUseCase createBudgetUseCase,
            UpdateBudgetUseCase updateBudgetUseCase,
            DeleteBudgetUseCase deleteBudgetUseCase,
            ListBudgetsUseCase listBudgetsUseCase,
            GetBudgetHistoryUseCase getBudgetHistoryUseCase) {
        this.createBudgetUseCase = createBudgetUseCase;
        this.updateBudgetUseCase = updateBudgetUseCase;
        this.deleteBudgetUseCase = deleteBudgetUseCase;
        this.listBudgetsUseCase = listBudgetsUseCase;
        this.getBudgetHistoryUseCase = getBudgetHistoryUseCase;
    }

    @PostMapping
    public ResponseEntity<CreateBudgetResponse> create(@Valid @RequestBody CreateBudgetRequest request, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        CreateBudgetUseCase.Output output = createBudgetUseCase.execute(new CreateBudgetUseCase.Input(
                userId,
                request.categoryId(),
                request.limitAmount(),
                request.periodType(),
                request.customPeriodStart(),
                request.customPeriodEnd(),
                request.alertThresholds()));

        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateBudgetResponse(output.budgetId()));
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> list(HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        ListBudgetsUseCase.Output output = listBudgetsUseCase.execute(new ListBudgetsUseCase.Input(userId));

        return ResponseEntity.ok(output.budgets().stream().map(BudgetResponse::from).toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable String id, @Valid @RequestBody UpdateBudgetRequest request, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        updateBudgetUseCase.execute(new UpdateBudgetUseCase.Input(userId, id, request.limitAmount(), request.alertThresholds()));

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        deleteBudgetUseCase.execute(new DeleteBudgetUseCase.Input(userId, id));

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<BudgetPeriodPerformanceResponse>> history(
            @PathVariable String id,
            @RequestParam(name = "periods", defaultValue = "6") int periods,
            HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        GetBudgetHistoryUseCase.Output output = getBudgetHistoryUseCase.execute(new GetBudgetHistoryUseCase.Input(userId, id, periods));

        return ResponseEntity.ok(output.periods().stream().map(BudgetPeriodPerformanceResponse::from).toList());
    }
}
