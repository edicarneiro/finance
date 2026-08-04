package com.financepulse.engine.adapters.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Uma rota inexistente (ex.: um navegador pedindo "/" ou "/favicon.ico" na
 * API) é um 404 comum, não um erro de aplicação — não deve cair no handler
 * genérico de 500 nem ser logada como ERROR (rules.md §5, observabilidade:
 * erro real vs. comportamento esperado de cliente).
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aNonExistentRouteReturns404NotInternalServerError() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isNotFound());
    }

    @Test
    void aNonExistentStaticResourceReturns404NotInternalServerError() throws Exception {
        mockMvc.perform(get("/favicon.ico")).andExpect(status().isNotFound());
    }
}
