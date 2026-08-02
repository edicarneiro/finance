package com.financepulse.engine.adapters.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Smoke test de ponta a ponta contra a raiz de composição real (rules.md §
 * 3). Usa datas fixas de 2026 (não relativas a {@code LocalDate.now()}) —
 * seguro porque os relatórios recebem período explícito do cliente, sem
 * depender de {@code Clock}/data atual (ver ADR-0021).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass1\"}"));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass1\"}"))
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
    }

    private String firstCategoryId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/categories").header("Authorization", "Bearer " + token)).andReturn();
        JsonNode categories = objectMapper.readTree(result.getResponse().getContentAsString());
        return categories.get(0).get("id").asText();
    }

    private String createAccount(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CHECKING\",\"name\":\"Conta Corrente\",\"currency\":\"BRL\",\"initialBalance\":0}"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accountId").asText();
    }

    private void createTransaction(String token, String accountId, String categoryId, String type, String amount, String date) throws Exception {
        mockMvc.perform(post("/transactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"" + accountId + "\",\"categoryId\":\"" + categoryId + "\",\"type\":\"" + type + "\",\"amount\":" + amount
                        + ",\"date\":\"" + date + "\",\"description\":null,\"tags\":[]}"));
    }

    @Test
    void rejectsAccessWithoutAToken() throws Exception {
        mockMvc.perform(get("/reports/spending-by-category").param("startDate", "2026-07-01").param("endDate", "2026-07-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void spendingByCategoryReflectsRealTransactionsWithinTheExplicitPeriod() throws Exception {
        String token = registerAndLogin("report-spending@example.com");
        String categoryId = firstCategoryId(token);
        String accountId = createAccount(token);

        createTransaction(token, accountId, categoryId, "EXPENSE", "200.00", "2026-07-15");
        createTransaction(token, accountId, categoryId, "EXPENSE", "999.00", "2026-06-01");

        mockMvc.perform(get("/reports/spending-by-category")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(200.00))
                .andExpect(jsonPath("$.categories[0].amount").value(200.00))
                .andExpect(jsonPath("$.categories[0].percentage").value(100.0));
    }

    @Test
    void rejectsAnInvertedPeriodWithBadRequest() throws Exception {
        String token = registerAndLogin("report-invalid-period@example.com");

        mockMvc.perform(get("/reports/spending-by-category")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", "2026-07-31")
                        .param("endDate", "2026-07-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void periodComparisonReflectsRealTransactionsFromBothPeriods() throws Exception {
        String token = registerAndLogin("report-comparison@example.com");
        String categoryId = firstCategoryId(token);
        String accountId = createAccount(token);

        createTransaction(token, accountId, categoryId, "EXPENSE", "200.00", "2026-06-15");
        createTransaction(token, accountId, categoryId, "EXPENSE", "300.00", "2026-07-15");

        mockMvc.perform(get("/reports/period-comparison")
                        .header("Authorization", "Bearer " + token)
                        .param("periodAStart", "2026-06-01")
                        .param("periodAEnd", "2026-06-30")
                        .param("periodBStart", "2026-07-01")
                        .param("periodBEnd", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodA.totalExpense").value(200.00))
                .andExpect(jsonPath("$.periodB.totalExpense").value(300.00))
                .andExpect(jsonPath("$.categoryComparisons[0].delta").value(100.00));
    }

    @Test
    void exportsSpendingByCategoryAsCsv() throws Exception {
        String token = registerAndLogin("report-export-category@example.com");
        String categoryId = firstCategoryId(token);
        String accountId = createAccount(token);
        createTransaction(token, accountId, categoryId, "EXPENSE", "150.00", "2026-07-10");

        mockMvc.perform(get("/reports/spending-by-category/export")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString()).contains("150.00"));
    }

    @Test
    void exportsTransactionsAsCsvWithAccountAndCategoryNames() throws Exception {
        String token = registerAndLogin("report-export-transactions@example.com");
        String categoryId = firstCategoryId(token);
        String accountId = createAccount(token);
        createTransaction(token, accountId, categoryId, "EXPENSE", "42.50", "2026-07-05");

        MvcResult result = mockMvc.perform(get("/reports/transactions/export")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(csv).contains("Conta Corrente").contains("42.50").contains("2026-07-05");
    }

    @Test
    void aUserNeverSeesAnotherUsersTransactionsInAReport() throws Exception {
        String ownerToken = registerAndLogin("report-owner@example.com");
        String otherToken = registerAndLogin("report-other@example.com");
        String categoryId = firstCategoryId(ownerToken);
        String accountId = createAccount(ownerToken);
        createTransaction(ownerToken, accountId, categoryId, "EXPENSE", "500.00", "2026-07-10");

        mockMvc.perform(get("/reports/spending-by-category")
                        .header("Authorization", "Bearer " + otherToken)
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(0));
    }
}
