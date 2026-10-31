package com.urlshortener.controller;

import com.urlshortener.exception.GlobalExceptionHandler;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.service.RateLimiterService;
import com.urlshortener.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP-level checks (validation + error mapping) for UrlController. */
@ExtendWith(MockitoExtension.class)
class UrlControllerMvcTest {

    @Mock
    private UrlService urlService;
    @Mock
    private RateLimiterService rateLimiterService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UrlController(urlService, rateLimiterService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shorten_rejectsNonHttpUrlWith400() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.longUrl").value("URL must start with http:// or https://"));
    }

    @Test
    void delete_returns404WhenUrlDoesNotExist() throws Exception {
        doThrow(new UrlNotFoundException("URL not found"))
                .when(urlService).deleteUserUrl(eq("abc"), any());

        mockMvc.perform(delete("/api/urls/abc"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("URL not found"));
    }
}
