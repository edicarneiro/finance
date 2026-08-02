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
 * 3), usando o {@code SystemClock} real — o prazo da meta é relativo a
 * {@code LocalDate.now()} para não depender de quando o teste roda.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GoalControllerTest {

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

    private String createAccount(String token, String initialBalance) throws Exception {
        MvcResult result = mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SAVINGS\",\"name\":\"Poupança\",\"currency\":\"BRL\",\"initialBalance\":" + initialBalance + "}"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accountId").asText();
    }

    private String firstCategoryId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/categories").header("Authorization", "Bearer " + token)).andReturn();
        JsonNode categories = objectMapper.readTree(result.getResponse().getContentAsString());
        return categories.get(0).get("id").asText();
    }

    @Test
    void rejectsAccessWithoutAToken() throws Exception {
        mockMvc.perform(get("/goals")).andExpect(status().isUnauthorized());
    }

    @Test
    void createsUpdatesAndDeletesAnAccountBasedGoal() throws Exception {
        String token = registerAndLogin("goal-owner@example.com");
        String accountId = createAccount(token, "400.00");
        String deadline = LocalDate.now().plusMonths(6).toString();

        MvcResult createResult = mockMvc.perform(post("/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Reserva\",\"targetAmount\":1000.00,\"deadline\":\"" + deadline + "\",\"accountId\":\"" + accountId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.goalId").exists())
                .andReturn();
        String goalId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("goalId").asText();

        mockMvc.perform(get("/goals").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentAmount").value(400.00))
                .andExpect(jsonPath("$[0].progressPercentage").value(40.0))
                .andExpect(jsonPath("$[0].achieved").value(false));

        mockMvc.perform(put("/goals/" + goalId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Reserva de emergência\",\"targetAmount\":2000.00,\"deadline\":\"" + deadline + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/goals").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].name").value("Reserva de emergência"))
                .andExpect(jsonPath("$[0].targetAmount").value(2000.00));

        mockMvc.perform(delete("/goals/" + goalId).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());

        mockMvc.perform(get("/goals").header("Authorization", "Bearer " + token)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void marksAGoalAsAchievedWhenTheAccountBalanceReachesTheTarget() throws Exception {
        String token = registerAndLogin("goal-achieved@example.com");
        String accountId = createAccount(token, "1000.00");
        String deadline = LocalDate.now().plusMonths(1).toString();

        mockMvc.perform(post("/goals")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Reserva\",\"targetAmount\":1000.00,\"deadline\":\"" + deadline + "\",\"accountId\":\"" + accountId + "\"}"));

        mockMvc.perform(get("/goals").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].achieved").value(true))
                .andExpect(jsonPath("$[0].thresholdsCrossed[0]").value(80));
    }

    @Test
    void rejectsCreatingAGoalWithBothAccountAndCategory() throws Exception {
        String token = registerAndLogin("goal-both@example.com");
        String accountId = createAccount(token, "0");
        String categoryId = firstCategoryId(token);
        String deadline = LocalDate.now().plusMonths(1).toString();

        mockMvc.perform(post("/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Meta\",\"targetAmount\":100.00,\"deadline\":\"" + deadline + "\",\"accountId\":\"" + accountId
                                + "\",\"categoryId\":\"" + categoryId + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCreatingAGoalWithAPastDeadline() throws Exception {
        String token = registerAndLogin("goal-past-deadline@example.com");
        String accountId = createAccount(token, "0");
        String pastDeadline = LocalDate.now().minusDays(1).toString();

        mockMvc.perform(post("/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Meta\",\"targetAmount\":100.00,\"deadline\":\"" + pastDeadline + "\",\"accountId\":\"" + accountId + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aUserCannotAccessAnotherUsersGoal() throws Exception {
        String ownerToken = registerAndLogin("goal-owner2@example.com");
        String intruderToken = registerAndLogin("goal-intruder@example.com");
        String accountId = createAccount(ownerToken, "0");
        String deadline = LocalDate.now().plusMonths(1).toString();

        MvcResult createResult = mockMvc.perform(post("/goals")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Meta\",\"targetAmount\":100.00,\"deadline\":\"" + deadline + "\",\"accountId\":\"" + accountId + "\"}"))
                .andReturn();
        String goalId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("goalId").asText();

        mockMvc.perform(put("/goals/" + goalId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Roubada\",\"targetAmount\":1.00,\"deadline\":\"" + deadline + "\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/goals/" + goalId).header("Authorization", "Bearer " + intruderToken)).andExpect(status().isNotFound());
    }
}
