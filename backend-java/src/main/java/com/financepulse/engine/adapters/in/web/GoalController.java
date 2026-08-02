package com.financepulse.engine.adapters.in.web;

import com.financepulse.engine.adapters.in.web.dto.CreateGoalRequest;
import com.financepulse.engine.adapters.in.web.dto.CreateGoalResponse;
import com.financepulse.engine.adapters.in.web.dto.GoalResponse;
import com.financepulse.engine.adapters.in.web.dto.UpdateGoalRequest;
import com.financepulse.engine.application.usecases.goal.CreateGoalUseCase;
import com.financepulse.engine.application.usecases.goal.DeleteGoalUseCase;
import com.financepulse.engine.application.usecases.goal.ListGoalsUseCase;
import com.financepulse.engine.application.usecases.goal.UpdateGoalUseCase;
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
import org.springframework.web.bind.annotation.RestController;

/** Rotas protegidas por {@link AuthenticationInterceptor}. RF-030, RF-031; RF-032 é apenas o sinal (ver ADR-0019). */
@RestController
@RequestMapping("/goals")
public class GoalController {

    private final CreateGoalUseCase createGoalUseCase;
    private final UpdateGoalUseCase updateGoalUseCase;
    private final DeleteGoalUseCase deleteGoalUseCase;
    private final ListGoalsUseCase listGoalsUseCase;

    public GoalController(
            CreateGoalUseCase createGoalUseCase,
            UpdateGoalUseCase updateGoalUseCase,
            DeleteGoalUseCase deleteGoalUseCase,
            ListGoalsUseCase listGoalsUseCase) {
        this.createGoalUseCase = createGoalUseCase;
        this.updateGoalUseCase = updateGoalUseCase;
        this.deleteGoalUseCase = deleteGoalUseCase;
        this.listGoalsUseCase = listGoalsUseCase;
    }

    @PostMapping
    public ResponseEntity<CreateGoalResponse> create(@Valid @RequestBody CreateGoalRequest request, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        CreateGoalUseCase.Output output = createGoalUseCase.execute(new CreateGoalUseCase.Input(
                userId,
                request.name(),
                request.targetAmount(),
                request.deadline(),
                request.accountId(),
                request.categoryId(),
                request.progressAlertThresholds()));

        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateGoalResponse(output.goalId()));
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> list(HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        ListGoalsUseCase.Output output = listGoalsUseCase.execute(new ListGoalsUseCase.Input(userId));

        return ResponseEntity.ok(output.goals().stream().map(GoalResponse::from).toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable String id, @Valid @RequestBody UpdateGoalRequest request, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        updateGoalUseCase.execute(new UpdateGoalUseCase.Input(
                userId, id, request.name(), request.targetAmount(), request.deadline(), request.progressAlertThresholds()));

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        deleteGoalUseCase.execute(new DeleteGoalUseCase.Input(userId, id));

        return ResponseEntity.noContent().build();
    }
}
