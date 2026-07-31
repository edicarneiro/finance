package com.financepulse.engine.adapters.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testa a raiz de composição real (contêiner Spring, H2, BCrypt, JWT), não
 * dublês — mesma disciplina do container.integration.test.ts do backend
 * TypeScript (rules.md §3): garante que a fiação real de ponta a ponta
 * funciona, não apenas os componentes isolados.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registersAndThenLogsInWithTheSameCredentials() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"e2e-user@example.com\",\"password\":\"StrongPass1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"e2e-user@example.com\",\"password\":\"StrongPass1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void rejectsRegistrationWithADuplicateEmail() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"dup-user@example.com\",\"password\":\"StrongPass1\"}"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup-user@example.com\",\"password\":\"AnotherPass1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void rejectsRegistrationWithAWeakPassword() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"weak-pass@example.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsLoginWithAWrongPassword() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"wrong-pass@example.com\",\"password\":\"StrongPass1\"}"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"wrong-pass@example.com\",\"password\":\"WrongPassword1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void rejectsLoginForANonExistentEmailWithTheSameErrorAsAWrongPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@example.com\",\"password\":\"whatever1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAMalformedRequestBodyWithoutLeaking500() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing-password@example.com\"}"))
                .andExpect(status().isBadRequest());
    }
}
