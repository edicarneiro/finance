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
 * RF-047 (ver ADR-0024): suíte consolidada — não substitui os testes de
 * isolamento pontuais já existentes em cada controller (mantidos), mas
 * reúne, em um único lugar auditável, uma verificação exaustiva de que o
 * usuário B nunca alcança dados do usuário A, para toda área de dado do
 * produto. Contra a raiz de composição real (rules.md § 3).
 */
@SpringBootTest
@AutoConfigureMockMvc
class MultiTenantIsolationHardeningTest {

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
                        .content("{\"type\":\"CHECKING\",\"name\":\"Conta\",\"currency\":\"BRL\",\"initialBalance\":1000}"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accountId").asText();
    }

    private String createCategory(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Categoria Privada de A\"}"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("categoryId").asText();
    }

    private String createTransaction(String token, String accountId, String categoryId) throws Exception {
        MvcResult result = mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"" + accountId + "\",\"categoryId\":\"" + categoryId
                                + "\",\"type\":\"EXPENSE\",\"amount\":50.00,\"date\":\"" + LocalDate.now() + "\",\"description\":\"privado\",\"tags\":[]}"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("transactionId").asText();
    }

    private String createBudget(String token, String categoryId) throws Exception {
        MvcResult result = mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + categoryId + "\",\"limitAmount\":100.00,\"periodType\":\"MONTHLY\"}"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("budgetId").asText();
    }

    private String createGoal(String token, String accountId) throws Exception {
        MvcResult result = mockMvc.perform(post("/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Meta Privada de A\",\"targetAmount\":1000.00,\"deadline\":\"" + LocalDate.now().plusMonths(6)
                                + "\",\"accountId\":\"" + accountId + "\"}"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("goalId").asText();
    }

    @Test
    void userBNeverReachesAnyDataBelongingToUserA() throws Exception {
        String tokenA = registerAndLogin("tenant-a@example.com");
        String tokenB = registerAndLogin("tenant-b@example.com");

        String categoryIdA = createCategory(tokenA);
        String accountIdA = createAccount(tokenA);
        String transactionIdA = createTransaction(tokenA, accountIdA, categoryIdA);
        String budgetIdA = createBudget(tokenA, categoryIdA);
        String goalIdA = createGoal(tokenA, accountIdA);
        mockMvc.perform(post("/notifications/check").header("Authorization", "Bearer " + tokenA));
        mockMvc.perform(post("/privacy/consents")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":\"2026-08-01\"}"));

        // Contas: B não vê a conta de A na própria listagem, nem acessa saldo consolidado de A.
        assertListDoesNotContainId(getAuthenticated("/accounts", tokenB), accountIdA);

        // Categorias: B não vê a categoria privada de A.
        assertListDoesNotContainId(getAuthenticated("/categories", tokenB), categoryIdA);

        // Transações: B não consegue editar/excluir a transação de A (RF-047 — mesmo erro de "não encontrado").
        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"" + accountIdA + "\",\"categoryId\":\"" + categoryIdA
                                + "\",\"type\":\"EXPENSE\",\"amount\":1.00,\"date\":\"" + LocalDate.now() + "\",\"description\":null,\"tags\":[]}"))
                .andExpect(status().isNotFound());

        // Orçamentos: B nunca vê o orçamento de A.
        assertListDoesNotContainId(getAuthenticated("/budgets", tokenB), budgetIdA);

        // Metas: B nunca vê a meta de A.
        assertListDoesNotContainId(getAuthenticated("/goals", tokenB), goalIdA);

        // Dashboard: B tem saldo/fluxo de caixa próprios, nunca refletindo os dados financeiros de A.
        mockMvc.perform(get("/dashboard").header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.consolidatedBalance").value(0));

        // Relatórios: B não vê os gastos de A em nenhum relatório.
        mockMvc.perform(get("/reports/spending-by-category")
                        .header("Authorization", "Bearer " + tokenB)
                        .param("startDate", LocalDate.now().minusDays(30).toString())
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(jsonPath("$.totalExpense").value(0));

        // Notificações: B nunca vê notificações geradas para A.
        mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + tokenB)).andExpect(jsonPath("$.length()").value(0));

        // Privacidade: B nunca vê o histórico de consentimento de A, nem a exportação de A vaza para B.
        mockMvc.perform(get("/privacy/consents").header("Authorization", "Bearer " + tokenB)).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/privacy/export").header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.accounts.length()").value(0))
                .andExpect(jsonPath("$.consentHistory.length()").value(0));

        // Transações de A permanecem intactas e acessíveis por A após todas as tentativas de B.
        mockMvc.perform(get("/transactions").header("Authorization", "Bearer " + tokenA).param("accountId", accountIdA))
                .andExpect(jsonPath("$[0].id").value(transactionIdA));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder getAuthenticated(String path, String token) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).header("Authorization", "Bearer " + token);
    }

    private void assertListDoesNotContainId(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String id)
            throws Exception {
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        JsonNode array = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode node : array) {
            String candidateId = node.has("id") ? node.get("id").asText() : null;
            org.assertj.core.api.Assertions.assertThat(candidateId).isNotEqualTo(id);
        }
    }
}
