package com.financepulse.engine.adapters.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Smoke test de ponta a ponta contra a raiz de composição real (rules.md § 3). RF-044/RF-046 (ver ADR-0023). */
@SpringBootTest
@AutoConfigureMockMvc
class PrivacyControllerTest {

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
                        .content("{\"type\":\"CHECKING\",\"name\":\"Conta Corrente\",\"currency\":\"BRL\",\"initialBalance\":0}"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accountId").asText();
    }

    @Test
    void rejectsAccessWithoutAToken() throws Exception {
        mockMvc.perform(get("/privacy/export")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/privacy/consents")).andExpect(status().isUnauthorized());
    }

    @Test
    void exportsRealAccountDataCreatedViaHttp() throws Exception {
        String token = registerAndLogin("export-me@example.com");
        createAccount(token);

        mockMvc.perform(get("/privacy/export").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.email").value("export-me@example.com"))
                .andExpect(jsonPath("$.accounts.length()").value(1))
                .andExpect(jsonPath("$.accounts[0].name").value("Conta Corrente"));
    }

    @Test
    void neverExposesThePasswordHashInTheExportedJson() throws Exception {
        String token = registerAndLogin("export-no-hash@example.com");

        MvcResult result = mockMvc.perform(get("/privacy/export").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(json.toLowerCase()).doesNotContain("passwordhash").doesNotContain("password_hash");
    }

    @Test
    void recordsAndListsConsentHistory() throws Exception {
        String token = registerAndLogin("consent-me@example.com");

        mockMvc.perform(post("/privacy/consents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"2026-08-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value("2026-08-01"));

        mockMvc.perform(get("/privacy/consents").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].version").value("2026-08-01"));
    }

    @Test
    void rejectsABlankConsentVersion() throws Exception {
        String token = registerAndLogin("consent-blank@example.com");

        mockMvc.perform(post("/privacy/consents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aUserNeverSeesAnotherUsersConsentHistoryOrExportedData() throws Exception {
        String ownerToken = registerAndLogin("privacy-owner@example.com");
        String otherToken = registerAndLogin("privacy-other@example.com");
        createAccount(ownerToken);
        mockMvc.perform(post("/privacy/consents")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":\"2026-08-01\"}"));

        mockMvc.perform(get("/privacy/export").header("Authorization", "Bearer " + otherToken))
                .andExpect(jsonPath("$.accounts.length()").value(0));
        mockMvc.perform(get("/privacy/consents").header("Authorization", "Bearer " + otherToken))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
