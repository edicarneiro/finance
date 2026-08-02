package com.financepulse.engine.adapters.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financepulse.engine.application.usecases.category.ListCategoriesUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerTest {

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

    @Test
    void rejectsAccessWithoutAToken() throws Exception {
        mockMvc.perform(get("/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void seedsAndReturnsTheDefaultCategoriesOnFirstAccess() throws Exception {
        String token = registerAndLogin("category-owner@example.com");

        mockMvc.perform(get("/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(ListCategoriesUseCase.DEFAULT_CATEGORY_NAMES.size()))
                .andExpect(jsonPath("$[0].name").value(ListCategoriesUseCase.DEFAULT_CATEGORY_NAMES.get(0)))
                .andExpect(jsonPath("$[0].parentCategoryId").doesNotExist());
    }

    @Test
    void createsRenamesAndDeletesACategory() throws Exception {
        String token = registerAndLogin("category-crud@example.com");

        MvcResult createResult = mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Assinaturas\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").exists())
                .andReturn();
        String categoryId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("categoryId").asText();

        mockMvc.perform(put("/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Assinaturas e Streaming\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/categories/" + categoryId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void createsASubcategoryOfAnExistingCategory() throws Exception {
        String token = registerAndLogin("category-sub@example.com");

        MvcResult parentResult = mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alimentação\"}"))
                .andReturn();
        String parentId = objectMapper.readTree(parentResult.getResponse().getContentAsString()).get("categoryId").asText();

        mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Restaurante\",\"parentCategoryId\":\"" + parentId + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/categories").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[?(@.name == 'Restaurante')].parentCategoryId").value(parentId));
    }

    @Test
    void rejectsDeletingACategoryThatHasSubcategories() throws Exception {
        String token = registerAndLogin("category-block-sub@example.com");

        MvcResult parentResult = mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alimentação\"}"))
                .andReturn();
        String parentId = objectMapper.readTree(parentResult.getResponse().getContentAsString()).get("categoryId").asText();
        mockMvc.perform(post("/categories")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Restaurante\",\"parentCategoryId\":\"" + parentId + "\"}"));

        mockMvc.perform(delete("/categories/" + parentId).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aUserCannotAccessAnotherUsersCategory() throws Exception {
        String ownerToken = registerAndLogin("category-owner2@example.com");
        String intruderToken = registerAndLogin("category-intruder@example.com");

        MvcResult createResult = mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Privada\"}"))
                .andReturn();
        String categoryId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("categoryId").asText();

        mockMvc.perform(put("/categories/" + categoryId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Roubada\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/categories/" + categoryId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }
}
