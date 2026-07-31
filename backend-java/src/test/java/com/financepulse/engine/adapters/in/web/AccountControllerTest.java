package com.financepulse.engine.adapters.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Testa a raiz de composição real (contêiner Spring, H2, BCrypt, JWT) de
 * ponta a ponta: registro → login → rota protegida de contas — primeira vez
 * que uma rota protegida de fato existe no backend Java (ver ADR-0014).
 * Mesma disciplina do AuthControllerTest/container.integration.test.ts
 * (rules.md § 3).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

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

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    @Test
    void rejectsAccessWithoutAToken() throws Exception {
        mockMvc.perform(get("/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAccessWithAnInvalidToken() throws Exception {
        mockMvc.perform(get("/accounts").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsListsAndConsolidatesAccountBalancesForTheAuthenticatedUser() throws Exception {
        String token = registerAndLogin("account-owner@example.com");

        mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CHECKING\",\"name\":\"Conta Corrente\",\"currency\":\"BRL\",\"initialBalance\":100.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").exists());

        mockMvc.perform(post("/accounts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"SAVINGS\",\"name\":\"Poupança\",\"currency\":\"BRL\",\"initialBalance\":250.50}"));

        mockMvc.perform(get("/accounts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/accounts/balance/consolidated").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consolidatedBalance").value(350.50));
    }

    @Test
    void updatesAndArchivesAnAccount() throws Exception {
        String token = registerAndLogin("account-editor@example.com");

        MvcResult createResult = mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CASH\",\"name\":\"Carteira\",\"currency\":\"BRL\",\"initialBalance\":0}"))
                .andReturn();
        String accountId =
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("accountId").asText();

        mockMvc.perform(put("/accounts/" + accountId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Carteira Principal\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/accounts/" + accountId + "/archive").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/accounts").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].name").value("Carteira Principal"))
                .andExpect(jsonPath("$[0].archived").value(true));
    }

    @Test
    void archivedAccountsAreExcludedFromTheConsolidatedBalance() throws Exception {
        String token = registerAndLogin("balance-owner@example.com");

        MvcResult createResult = mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CASH\",\"name\":\"Carteira\",\"currency\":\"BRL\",\"initialBalance\":500.00}"))
                .andReturn();
        String accountId =
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("accountId").asText();

        mockMvc.perform(post("/accounts/" + accountId + "/archive").header("Authorization", "Bearer " + token));

        mockMvc.perform(get("/accounts/balance/consolidated").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.consolidatedBalance").value(0));
    }

    @Test
    void aUserCannotAccessAnotherUsersAccount() throws Exception {
        String ownerToken = registerAndLogin("owner@example.com");
        String intruderToken = registerAndLogin("intruder@example.com");

        MvcResult createResult = mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CASH\",\"name\":\"Carteira\",\"currency\":\"BRL\",\"initialBalance\":0}"))
                .andReturn();
        String accountId =
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("accountId").asText();

        mockMvc.perform(put("/accounts/" + accountId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Conta Roubada\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/accounts/" + accountId + "/archive").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsCreatingAnAccountWithAnInvalidCurrency() throws Exception {
        String token = registerAndLogin("invalid-currency@example.com");

        mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CASH\",\"name\":\"Carteira\",\"currency\":\"REAIS\",\"initialBalance\":0}"))
                .andExpect(status().isBadRequest());
    }
}
