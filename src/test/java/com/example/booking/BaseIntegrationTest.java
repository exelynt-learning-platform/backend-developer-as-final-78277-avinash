package com.example.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public abstract class BaseIntegrationTest {

    @Autowired
    protected WebApplicationContext context;

    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    protected static final String ADMIN_USERNAME = "admin@example.com";
    protected static final String ADMIN_PASSWORD = "Admin@123";
    protected static final String USER_USERNAME = "user@example.com";
    protected static final String USER_PASSWORD = "User@123";

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    protected String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginPayload(username, password));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseJson).get("token").asText();
    }

    protected String adminToken() throws Exception {
        return loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    protected String userToken() throws Exception {
        return loginAndGetToken(USER_USERNAME, USER_PASSWORD);
    }

    private record LoginPayload(String username, String password) {
    }
}