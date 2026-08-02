package com.financepulse.engine.adapters.in.web;

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
 * 3), usando o {@code SystemClock} real — datas de transação/orçamento
 * relativas a {@code LocalDate.now()} para não depender de quando o teste
 * roda.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

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
        mockMvc.perform(get("/notification-preferences")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void preferencesDefaultToEnabledAndCanBeUpdated() throws Exception {
        String token = registerAndLogin("notif-prefs@example.com");

        mockMvc.perform(get("/notification-preferences").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[?(@.enabled == false)]").isEmpty());

        mockMvc.perform(put("/notification-preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"alertType\":\"BUDGET_THRESHOLD\",\"channel\":\"EMAIL\",\"enabled\":false}]"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/notification-preferences").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[?(@.alertType == 'BUDGET_THRESHOLD' && @.channel == 'EMAIL')].enabled").value(false));
    }

    @Test
    void checkDetectsARealBudgetThresholdCrossingAndItAppearsInTheInboxUnreadThenRead() throws Exception {
        String token = registerAndLogin("notif-check@example.com");
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
                        + "\",\"type\":\"EXPENSE\",\"amount\":90.00,\"date\":\"" + today + "\",\"description\":null,\"tags\":[]}"));

        MvcResult checkResult = mockMvc.perform(post("/notifications/check").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode checked = objectMapper.readTree(checkResult.getResponse().getContentAsString());
        assertThatArrayIsNotEmpty(checked);

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].read").value(false));

        String notificationId = objectMapper.readTree(mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + token)).andReturn()
                .getResponse().getContentAsString()).get(0).get("id").asText();

        mockMvc.perform(put("/notifications/" + notificationId + "/read").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + token).param("unreadOnly", "true"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void markingAnotherUsersNotificationAsReadReturnsNotFound() throws Exception {
        String ownerToken = registerAndLogin("notif-owner@example.com");
        String intruderToken = registerAndLogin("notif-intruder@example.com");
        String categoryId = firstCategoryId(ownerToken);
        String accountId = createAccount(ownerToken);
        String today = LocalDate.now().toString();

        mockMvc.perform(post("/budgets")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":\"" + categoryId + "\",\"limitAmount\":100.00,\"periodType\":\"MONTHLY\",\"alertThresholds\":[80,100]}"));
        mockMvc.perform(post("/transactions")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"" + accountId + "\",\"categoryId\":\"" + categoryId
                        + "\",\"type\":\"EXPENSE\",\"amount\":90.00,\"date\":\"" + today + "\",\"description\":null,\"tags\":[]}"));
        mockMvc.perform(post("/notifications/check").header("Authorization", "Bearer " + ownerToken));

        String notificationId = objectMapper.readTree(mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + ownerToken)).andReturn()
                .getResponse().getContentAsString()).get(0).get("id").asText();

        mockMvc.perform(put("/notifications/" + notificationId + "/read").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    private static void assertThatArrayIsNotEmpty(JsonNode array) {
        org.assertj.core.api.Assertions.assertThat(array.size()).isGreaterThan(0);
    }
}
