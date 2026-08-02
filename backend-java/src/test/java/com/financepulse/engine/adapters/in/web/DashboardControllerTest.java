package com.financepulse.engine.adapters.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Smoke test de ponta a ponta contra a raiz de composição real (rules.md §
 * 3), usando o {@code SystemClock} real — as datas de transação usadas são
 * relativas a {@code LocalDate.now()} para não depender de quando o teste roda.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

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

    private String createAccount(String token, String initialBalance) throws Exception {
        MvcResult result = mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CHECKING\",\"name\":\"Conta Corrente\",\"currency\":\"BRL\",\"initialBalance\":" + initialBalance + "}"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accountId").asText();
    }

    @Test
    void rejectsAccessWithoutAToken() throws Exception {
        mockMvc.perform(get("/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void aggregatesRealBalanceCashFlowAndSpendingFromTransactionsCreatedViaHttp() throws Exception {
        String token = registerAndLogin("dashboard-owner@example.com");
        String categoryId = firstCategoryId(token);
        String accountId = createAccount(token, "1000.00");
        String today = LocalDate.now().toString();

        mockMvc.perform(post("/transactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"" + accountId + "\",\"categoryId\":\"" + categoryId
                        + "\",\"type\":\"INCOME\",\"amount\":500.00,\"date\":\"" + today + "\",\"description\":null,\"tags\":[]}"));
        mockMvc.perform(post("/transactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"" + accountId + "\",\"categoryId\":\"" + categoryId
                        + "\",\"type\":\"EXPENSE\",\"amount\":200.00,\"date\":\"" + today + "\",\"description\":null,\"tags\":[]}"));

        mockMvc.perform(get("/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consolidatedBalance").value(1300.00))
                .andExpect(jsonPath("$.cashFlow.totalIncome").value(500.00))
                .andExpect(jsonPath("$.cashFlow.totalExpense").value(200.00))
                .andExpect(jsonPath("$.cashFlow.net").value(300.00))
                .andExpect(jsonPath("$.spendingByCategory[0].categoryId").value(categoryId))
                .andExpect(jsonPath("$.spendingByCategory[0].amount").value(200.00))
                .andExpect(jsonPath("$.pulseScore.formulaVersion").value("pulse-v0-provisional"))
                .andExpect(jsonPath("$.pulseScore.factors").isArray());
    }

    @Test
    void aFreshUserWithNoDataGetsAZeroedDashboardWithoutError() throws Exception {
        String token = registerAndLogin("dashboard-empty@example.com");

        mockMvc.perform(get("/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consolidatedBalance").value(0))
                .andExpect(jsonPath("$.cashFlow.totalIncome").value(0))
                .andExpect(jsonPath("$.spendingByCategory").isArray())
                .andExpect(jsonPath("$.spendingByCategory.length()").value(0))
                .andExpect(jsonPath("$.pulseScore.overallScore").value(50.0));
    }

    @Test
    void pulseScoreHistoryReflectsTheSnapshotWrittenByTheDashboardCall() throws Exception {
        String token = registerAndLogin("dashboard-history@example.com");

        mockMvc.perform(get("/dashboard").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        mockMvc.perform(get("/dashboard/pulse-score/history").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].date").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$[0].formulaVersion").value("pulse-v0-provisional"));
    }

    @Test
    void aUserNeverSeesAnotherUsersDashboardData() throws Exception {
        String ownerToken = registerAndLogin("dashboard-owner2@example.com");
        String otherToken = registerAndLogin("dashboard-other@example.com");
        createAccount(ownerToken, "5000.00");

        mockMvc.perform(get("/dashboard").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consolidatedBalance").value(0));
    }
}
