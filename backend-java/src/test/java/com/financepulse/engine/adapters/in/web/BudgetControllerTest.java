package com.financepulse.engine.adapters.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class BudgetControllerTest {

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

    @Test
    void rejectsAccessWithoutAToken() throws Exception {
        mockMvc.perform(get("/budgets")).andExpect(status().isUnauthorized());
    }

    @Test
    void createsUpdatesAndDeletesABudget() throws Exception {
        String token = registerAndLogin("budget-owner@example.com");
        String categoryId = firstCategoryId(token);

        MvcResult createResult = mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + categoryId + "\",\"limitAmount\":500.00,\"periodType\":\"MONTHLY\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.budgetId").exists())
                .andReturn();
        String budgetId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("budgetId").asText();

        mockMvc.perform(get("/budgets").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].limitAmount").value(500.00))
                .andExpect(jsonPath("$[0].periodType").value("MONTHLY"))
                .andExpect(jsonPath("$[0].alertThresholds").isArray());

        mockMvc.perform(put("/budgets/" + budgetId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limitAmount\":800.00,\"alertThresholds\":[50]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/budgets").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].limitAmount").value(800.00))
                .andExpect(jsonPath("$[0].alertThresholds[0]").value(50));

        mockMvc.perform(delete("/budgets/" + budgetId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/budgets").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aBudgetReflectsRealTransactionConsumptionAndCrossedThresholds() throws Exception {
        String token = registerAndLogin("budget-consumption@example.com");
        String categoryId = firstCategoryId(token);
        String accountId = createAccount(token);
        String today = LocalDate.now().toString();

        mockMvc.perform(post("/budgets")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":\"" + categoryId + "\",\"limitAmount\":100.00,\"periodType\":\"MONTHLY\",\"alertThresholds\":[80,100]}"));

        mockMvc.perform(post("/transactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"" + accountId + "\",\"categoryId\":\"" + categoryId
                        + "\",\"type\":\"EXPENSE\",\"amount\":85.00,\"date\":\"" + today + "\",\"description\":null,\"tags\":[]}"));

        mockMvc.perform(get("/budgets").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].consumedAmount").value(85.00))
                .andExpect(jsonPath("$[0].thresholdsCrossed[0]").value(80));
    }

    @Test
    void historyEndpointReturnsThePastPeriods() throws Exception {
        String token = registerAndLogin("budget-history@example.com");
        String categoryId = firstCategoryId(token);

        MvcResult createResult = mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + categoryId + "\",\"limitAmount\":100.00,\"periodType\":\"MONTHLY\"}"))
                .andReturn();
        String budgetId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("budgetId").asText();

        mockMvc.perform(get("/budgets/" + budgetId + "/history").param("periods", "3").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void aUserCannotAccessAnotherUsersBudget() throws Exception {
        String ownerToken = registerAndLogin("budget-owner2@example.com");
        String intruderToken = registerAndLogin("budget-intruder@example.com");
        String categoryId = firstCategoryId(ownerToken);

        MvcResult createResult = mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + categoryId + "\",\"limitAmount\":100.00,\"periodType\":\"MONTHLY\"}"))
                .andReturn();
        String budgetId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("budgetId").asText();

        mockMvc.perform(put("/budgets/" + budgetId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limitAmount\":1.00}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/budgets/" + budgetId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsCreatingABudgetForANonExistentCategory() throws Exception {
        String token = registerAndLogin("budget-ghost-category@example.com");

        mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"ghost-category\",\"limitAmount\":100.00,\"periodType\":\"MONTHLY\"}"))
                .andExpect(status().isNotFound());
    }
}
