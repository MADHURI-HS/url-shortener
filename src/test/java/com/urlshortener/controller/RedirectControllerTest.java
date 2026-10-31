package com.urlshortener.controller;

import com.urlshortener.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RedirectControllerTest {

    @Mock
    private UrlService urlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RedirectController(urlService)).build();
    }

    @Test
    void knownCode_redirectsWith302() throws Exception {
        when(urlService.getLongUrl("abc")).thenReturn("https://example.com");

        mockMvc.perform(get("/abc"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));
    }

    @Test
    void unknownCode_returns404() throws Exception {
        when(urlService.getLongUrl("nope")).thenReturn(null);

        mockMvc.perform(get("/nope"))
                .andExpect(status().isNotFound());
    }
}
