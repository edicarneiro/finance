package com.financepulse.engine.adapters.in.web;

import com.financepulse.engine.adapters.in.web.dto.PeriodComparisonReportResponse;
import com.financepulse.engine.adapters.in.web.dto.SpendingByCategoryReportResponse;
import com.financepulse.engine.application.usecases.report.GetPeriodComparisonReportUseCase;
import com.financepulse.engine.application.usecases.report.GetSpendingByCategoryReportUseCase;
import com.financepulse.engine.application.usecases.report.GetTransactionsForPeriodUseCase;
import com.financepulse.engine.application.usecases.report.GetTransactionsForPeriodUseCase.TransactionRow;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rotas protegidas por {@link AuthenticationInterceptor}. RF-037, RF-038, RF-039 (CSV — PDF fora do escopo, ver ADR-0021). */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final GetSpendingByCategoryReportUseCase getSpendingByCategoryReportUseCase;
    private final GetPeriodComparisonReportUseCase getPeriodComparisonReportUseCase;
    private final GetTransactionsForPeriodUseCase getTransactionsForPeriodUseCase;

    public ReportController(
            GetSpendingByCategoryReportUseCase getSpendingByCategoryReportUseCase,
            GetPeriodComparisonReportUseCase getPeriodComparisonReportUseCase,
            GetTransactionsForPeriodUseCase getTransactionsForPeriodUseCase) {
        this.getSpendingByCategoryReportUseCase = getSpendingByCategoryReportUseCase;
        this.getPeriodComparisonReportUseCase = getPeriodComparisonReportUseCase;
        this.getTransactionsForPeriodUseCase = getTransactionsForPeriodUseCase;
    }

    @GetMapping("/spending-by-category")
    public ResponseEntity<SpendingByCategoryReportResponse> spendingByCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        GetSpendingByCategoryReportUseCase.Output output =
                getSpendingByCategoryReportUseCase.execute(new GetSpendingByCategoryReportUseCase.Input(userId, startDate, endDate));

        return ResponseEntity.ok(SpendingByCategoryReportResponse.from(output));
    }

    @GetMapping(value = "/spending-by-category/export", produces = "text/csv")
    public ResponseEntity<String> exportSpendingByCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        GetSpendingByCategoryReportUseCase.Output output =
                getSpendingByCategoryReportUseCase.execute(new GetSpendingByCategoryReportUseCase.Input(userId, startDate, endDate));

        List<String> header = List.of("categoryId", "categoryName", "amount", "percentage");
        List<List<String>> rows = output.categories().stream()
                .map(category -> List.of(
                        category.categoryId(), category.categoryName(), category.amount().toPlainString(), category.percentage().toPlainString()))
                .toList();

        return csvResponse("gastos-por-categoria_" + startDate + "_" + endDate + ".csv", CsvWriter.write(header, rows));
    }

    @GetMapping("/period-comparison")
    public ResponseEntity<PeriodComparisonReportResponse> periodComparison(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodAStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodAEnd,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodBStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodBEnd,
            HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        GetPeriodComparisonReportUseCase.Output output = getPeriodComparisonReportUseCase.execute(
                new GetPeriodComparisonReportUseCase.Input(userId, periodAStart, periodAEnd, periodBStart, periodBEnd));

        return ResponseEntity.ok(PeriodComparisonReportResponse.from(output));
    }

    @GetMapping(value = "/transactions/export", produces = "text/csv")
    public ResponseEntity<String> exportTransactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        GetTransactionsForPeriodUseCase.Output output =
                getTransactionsForPeriodUseCase.execute(new GetTransactionsForPeriodUseCase.Input(userId, startDate, endDate));

        List<String> header = List.of("date", "accountName", "categoryName", "type", "amount", "description", "tags");
        List<List<String>> rows = output.transactions().stream().map(ReportController::toRow).toList();

        return csvResponse("transacoes_" + startDate + "_" + endDate + ".csv", CsvWriter.write(header, rows));
    }

    private static List<String> toRow(TransactionRow transaction) {
        return List.of(
                transaction.date().toString(),
                transaction.accountName(),
                transaction.categoryName(),
                transaction.type().toString(),
                transaction.amount().toPlainString(),
                transaction.description() == null ? "" : transaction.description(),
                String.join(";", transaction.tags()));
    }

    private static ResponseEntity<String> csvResponse(String filename, String csv) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
