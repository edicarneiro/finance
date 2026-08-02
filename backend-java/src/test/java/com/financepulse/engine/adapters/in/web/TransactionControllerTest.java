package com.financepulse.engine.adapters.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
 * Smoke test de ponta a ponta contra a raiz de composição real (rules.md § 3):
 * registro → login → categorias padrão → conta → transação → saldo refletido.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

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

    private String createAccount(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CHECKING\",\"name\":\"Conta Corrente\",\"currency\":\"BRL\",\"initialBalance\":100.00}"))
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
        mockMvc.perform(get("/transactions").param("accountId", "any")).andExpect(status().isUnauthorized());
    }

    @Test
    void createsListsUpdatesAndDeletesATransaction() throws Exception {
        String token = registerAndLogin("tx-owner@example.com");
        String accountId = createAccount(token);
        String categoryId = firstCategoryId(token);

        MvcResult createResult = mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"" + accountId + "\",\"categoryId\":\"" + categoryId
                                + "\",\"type\":\"EXPENSE\",\"amount\":49.90,\"date\":\"2026-07-01\",\"description\":\"Mercado\",\"tags\":[\"essencial\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").exists())
                .andReturn();
        String transactionId =
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("transactionId").asText();

        mockMvc.perform(get("/transactions").param("accountId", accountId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Mercado"));

        mockMvc.perform(put("/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"" + accountId + "\",\"categoryId\":\"" + categoryId
                                + "\",\"type\":\"EXPENSE\",\"amount\":60.00,\"date\":\"2026-07-02\",\"description\":\"Mercado editado\",\"tags\":[]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions").param("accountId", accountId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].description").value("Mercado editado"))
                .andExpect(jsonPath("$[0].amount").value(60.00));

        mockMvc.perform(delete("/transactions/" + transactionId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/transactions").param("accountId", accountId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aTransactionAffectsTheAccountsCurrentBalance() throws Exception {
        String token = registerAndLogin("tx-balance@example.com");
        String accountId = createAccount(token);
        String categoryId = firstCategoryId(token);

        mockMvc.perform(post("/transactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"" + accountId + "\",\"categoryId\":\"" + categoryId
                        + "\",\"type\":\"EXPENSE\",\"amount\":30.00,\"date\":\"2026-07-01\",\"description\":null,\"tags\":[]}"));
        mockMvc.perform(post("/transactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"" + accountId + "\",\"categoryId\":\"" + categoryId
                        + "\",\"type\":\"INCOME\",\"amount\":10.00,\"date\":\"2026-07-01\",\"description\":null,\"tags\":[]}"));

        mockMvc.perform(get("/accounts").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].balance").value(80.00));

        mockMvc.perform(get("/accounts/balance/consolidated").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.consolidatedBalance").value(80.00));
    }

    @Test
    void rejectsCreatingATransactionOnAnArchivedAccount() throws Exception {
        String token = registerAndLogin("tx-archived@example.com");
        String accountId = createAccount(token);
        String categoryId = firstCategoryId(token);
        mockMvc.perform(post("/accounts/" + accountId + "/archive").header("Authorization", "Bearer " + token));

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"" + accountId + "\",\"categoryId\":\"" + categoryId
                                + "\",\"type\":\"EXPENSE\",\"amount\":10.00,\"date\":\"2026-07-01\",\"description\":null,\"tags\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aUserCannotAccessAnotherUsersTransaction() throws Exception {
        String ownerToken = registerAndLogin("tx-owner2@example.com");
        String intruderToken = registerAndLogin("tx-intruder@example.com");
        String accountId = createAccount(ownerToken);
        String categoryId = firstCategoryId(ownerToken);

        MvcResult createResult = mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"" + accountId + "\",\"categoryId\":\"" + categoryId
                                + "\",\"type\":\"EXPENSE\",\"amount\":10.00,\"date\":\"2026-07-01\",\"description\":null,\"tags\":[]}"))
                .andReturn();
        String transactionId =
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("transactionId").asText();

        mockMvc.perform(delete("/transactions/" + transactionId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/transactions").param("accountId", accountId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }
}
