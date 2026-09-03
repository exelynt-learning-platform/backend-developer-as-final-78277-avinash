package com.example.booking.controller;

import com.example.booking.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ResourceControllerTest extends BaseIntegrationTest {

    @Test
    void getResources_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getResources_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getResources_asUser_isAllowed() throws Exception {
        String token = userToken();
        mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getResources_asAdmin_isAllowed() throws Exception {
        String token = adminToken();
        mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void createResource_asUser_returns403() throws Exception {
        String token = userToken();
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"name":"Test Room","available":true,"price":100.00}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createResource_asAdmin_returns201() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"name":"Test Room","description":"desc","type":"ROOM","available":true,"price":100.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Test Room"));
    }

    @Test
    void createResource_withNegativePrice_returns400() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"name":"Bad Room","available":true,"price":-50.00}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createResource_withMissingName_returns400() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"available":true,"price":100.00}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateResource_asUser_returns403() throws Exception {
        String userTok = userToken();
        String adminTok = adminToken();

        // create as admin first
        String created = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType("application/json")
                        .content("""
                                {"name":"Editable Room","available":true,"price":100.00}
                                """))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(put("/api/resources/" + id)
                        .header("Authorization", "Bearer " + userTok)
                        .contentType("application/json")
                        .content("""
                                {"name":"Hacked Room","available":true,"price":1.00}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteResource_asAdmin_returns204() throws Exception {
        String adminTok = adminToken();

        String created = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType("application/json")
                        .content("""
                                {"name":"Deletable Room","available":true,"price":100.00}
                                """))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(delete("/api/resources/" + id)
                        .header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isNoContent());
    }

    @Test
    void getResourceById_notFound_returns404() throws Exception {
        String token = userToken();
        mockMvc.perform(get("/api/resources/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
