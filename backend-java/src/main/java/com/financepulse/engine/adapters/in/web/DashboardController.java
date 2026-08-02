package com.financepulse.engine.adapters.in.web;

import com.financepulse.engine.adapters.in.web.dto.DashboardResponse;
import com.financepulse.engine.adapters.in.web.dto.PulseScoreHistoryEntryResponse;
import com.financepulse.engine.application.usecases.dashboard.GetDashboardUseCase;
import com.financepulse.engine.application.usecases.dashboard.GetPulseScoreHistoryUseCase;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rotas protegidas por {@link AuthenticationInterceptor}. RF-033, RF-034, RF-035, RF-036 (ver ADR-0020). */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final GetDashboardUseCase getDashboardUseCase;
    private final GetPulseScoreHistoryUseCase getPulseScoreHistoryUseCase;

    public DashboardController(GetDashboardUseCase getDashboardUseCase, GetPulseScoreHistoryUseCase getPulseScoreHistoryUseCase) {
        this.getDashboardUseCase = getDashboardUseCase;
        this.getPulseScoreHistoryUseCase = getPulseScoreHistoryUseCase;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> get(@RequestParam(name = "days", defaultValue = "30") int days, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        GetDashboardUseCase.Output output = getDashboardUseCase.execute(new GetDashboardUseCase.Input(userId, days));

        return ResponseEntity.ok(DashboardResponse.from(output));
    }

    @GetMapping("/pulse-score/history")
    public ResponseEntity<List<PulseScoreHistoryEntryResponse>> pulseScoreHistory(
            @RequestParam(name = "days", defaultValue = "90") int days, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        GetPulseScoreHistoryUseCase.Output output = getPulseScoreHistoryUseCase.execute(new GetPulseScoreHistoryUseCase.Input(userId, days));

        return ResponseEntity.ok(output.history().stream().map(PulseScoreHistoryEntryResponse::from).toList());
    }
}
