package com.financepulse.engine.adapters.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.domain.user.Role;
import com.financepulse.engine.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Smoke test de ponta a ponta contra a raiz de composição real (rules.md §
 * 3). RF-048/RF-049/RF-050 — versão mínima manual (ver ADR-0024): a
 * promoção a {@code SUPPORT_OPERATOR} é simulada aqui via {@link UserRepository}
 * injetado diretamente, exatamente como a promoção manual/fora de banda
 * descrita na decisão arquitetural (não existe endpoint para isso).
 */
@SpringBootTest
@AutoConfigureMockMvc
class BackofficeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

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

    private void promoteToSupportOperator(String email) {
        User operator = userRepository.findByEmail(com.financepulse.engine.domain.user.Email.create(email)).orElseThrow();
        userRepository.update(User.reconstitute(
                operator.getId(), operator.getEmail(), operator.getPasswordHash(), operator.getName(), operator.getCreatedAt(), null,
                Role.SUPPORT_OPERATOR, null));
    }

    @Test
    void rejectsAccessWithoutAToken() throws Exception {
        mockMvc.perform(get("/backoffice/users/some-id")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsBackofficeAccessForARegularCustomer() throws Exception {
        String customerToken = registerAndLogin("customer-no-role@example.com");
        String targetToken = registerAndLogin("target-1@example.com");

        mockMvc.perform(get("/backoffice/users/x").header("Authorization", "Bearer " + customerToken)).andExpect(status().isForbidden());
    }

    @Test
    void anOperatorCanViewATargetUsersDataAndItIsAudited() throws Exception {
        String operatorEmail = "operator-view@example.com";
        String operatorToken = registerAndLogin(operatorEmail);
        promoteToSupportOperator(operatorEmail);

        String targetToken = registerAndLogin("target-view@example.com");
        User target = userRepository.findByEmail(com.financepulse.engine.domain.user.Email.create("target-view@example.com")).orElseThrow();

        mockMvc.perform(get("/backoffice/users/" + target.getId()).header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.email").value("target-view@example.com"));

        mockMvc.perform(get("/backoffice/users/" + target.getId() + "/audit-log").header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].action").value("VIEWED_USER_DATA"));
    }

    @Test
    void anOperatorCanSuspendAndReactivateAnAccountBlockingAndRestoringLogin() throws Exception {
        String operatorEmail = "operator-suspend@example.com";
        String operatorToken = registerAndLogin(operatorEmail);
        promoteToSupportOperator(operatorEmail);

        String targetEmail = "target-suspend@example.com";
        registerAndLogin(targetEmail);
        User target = userRepository.findByEmail(com.financepulse.engine.domain.user.Email.create(targetEmail)).orElseThrow();

        mockMvc.perform(post("/backoffice/users/" + target.getId() + "/suspend")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Suspeita de fraude\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + targetEmail + "\",\"password\":\"StrongPass1\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/backoffice/users/" + target.getId() + "/reactivate")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Suspeita descartada\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + targetEmail + "\",\"password\":\"StrongPass1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void aCustomerCannotSuspendAnotherAccount() throws Exception {
        String customerToken = registerAndLogin("customer-cannot-suspend@example.com");
        registerAndLogin("target-safe@example.com");
        User target = userRepository.findByEmail(com.financepulse.engine.domain.user.Email.create("target-safe@example.com")).orElseThrow();

        mockMvc.perform(post("/backoffice/users/" + target.getId() + "/suspend")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"tentativa indevida\"}"))
                .andExpect(status().isForbidden());

        assertThatTargetIsNotSuspended(target.getId());
    }

    private void assertThatTargetIsNotSuspended(String userId) {
        org.assertj.core.api.Assertions.assertThat(userRepository.findById(userId).orElseThrow().isSuspended()).isFalse();
    }
}
