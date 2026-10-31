package com.urlshortener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test through the real Spring context (security filter chain, JWT,
 * JPA on in-memory H2, Spring Cache). Needs no MySQL or Redis.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UrlShortenerFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String credentials = "{\"name\":\"Jane\",\"email\":\"" + email + "\",\"password\":\"secret123\"}";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isOk());

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(login.getResponse().getContentAsString());
        return json.get("data").get("token").asText();
    }

    private String shorten(String token, String longUrl, String clientIp) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/shorten")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"" + longUrl + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String shortUrl = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("shortUrl").asText();
        return shortUrl.substring(shortUrl.lastIndexOf('/') + 1);
    }

    @Test
    void fullLifecycle_registerLoginShortenRedirectDelete() throws Exception {
        String token = registerAndLogin();

        String code = shorten(token, "https://example.com/lifecycle", "10.1.0.1");

        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/lifecycle"));

        mockMvc.perform(get("/api/urls").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].shortCode").value(code));

        mockMvc.perform(delete("/api/urls/" + code).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // the redirect was cached; deleting must evict it so the link dies immediately
        mockMvc.perform(get("/" + code))
                .andExpect(status().isNotFound());
    }

    @Test
    void sameUrlShortenedTwice_returnsSameCode() throws Exception {
        String token = registerAndLogin();

        String first = shorten(token, "https://example.com/duplicate", "10.2.0.1");
        String second = shorten(token, "https://example.com/duplicate", "10.2.0.1");

        assertEquals(first, second);
    }

    @Test
    void getUrls_withoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/urls"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidToken_isUnauthorizedNotServerError() throws Exception {
        mockMvc.perform(get("/api/urls").header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shorten_withoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"https://example.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sixthShortenRequestFromSameIpIsRateLimited() throws Exception {
        String token = registerAndLogin();

        for (int i = 1; i <= 5; i++) {
            shorten(token, "https://example.com/rate-" + i, "10.3.0.1");
        }

        mockMvc.perform(post("/api/shorten")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-For", "10.3.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"https://example.com/rate-6\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void registeringTwoUsersWithTheSameNameIsAllowed() throws Exception {
        registerAndLogin();
        registerAndLogin(); // both use the name "Jane"
    }

    @Test
    void unknownShortCode_returns404() throws Exception {
        mockMvc.perform(get("/doesnotexist"))
                .andExpect(status().isNotFound());
    }
}
