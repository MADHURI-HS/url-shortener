package com.urlshortener.controller;

import com.urlshortener.dto.ApiResponse;
import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.model.User;
import com.urlshortener.service.RateLimiterService;
import com.urlshortener.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlControllerTest {

    @Mock
    private UrlService urlService;
    @Mock
    private RateLimiterService rateLimiterService;

    private UrlController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new UrlController(urlService, rateLimiterService);
        ReflectionTestUtils.setField(controller, "baseUrl", "http://localhost:8080");
        user = User.builder().id(1L).email("jane@example.com").role("ROLE_USER").build();
    }

    private ShortenRequest body(String longUrl) {
        ShortenRequest r = new ShortenRequest();
        r.setLongUrl(longUrl);
        return r;
    }

    @Test
    void shorten_returnsShortUrlWhenUnderRateLimit() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1");
        when(rateLimiterService.tryConsume("10.0.0.1")).thenReturn(true);
        when(urlService.shortenUrl("https://example.com", user)).thenReturn("abc");

        ResponseEntity<?> response = controller.shortenUrl(body("https://example.com"), request, user);

        assertEquals(200, response.getStatusCode().value());
        ApiResponse<?> api = (ApiResponse<?>) response.getBody();
        assertTrue(api.success());
        ShortenResponse data = (ShortenResponse) api.data();
        assertEquals("http://localhost:8080/abc", data.getShortUrl());
        assertEquals("https://example.com", data.getLongUrl());
    }

    @Test
    void shorten_returns429WhenRateLimitExceeded() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1");
        when(rateLimiterService.tryConsume("10.0.0.1")).thenReturn(false);

        ResponseEntity<?> response = controller.shortenUrl(body("https://example.com"), request, user);

        assertEquals(429, response.getStatusCode().value());
        ApiResponse<?> api = (ApiResponse<?>) response.getBody();
        assertFalse(api.success());
        assertEquals("Too many requests. Try again in 1 minute.", api.message());
        verifyNoInteractions(urlService);
    }

    @Test
    void shorten_fallsBackToRemoteAddressWithoutForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.5");
        when(rateLimiterService.tryConsume("192.168.1.5")).thenReturn(true);
        when(urlService.shortenUrl(any(), any())).thenReturn("abc");

        controller.shortenUrl(body("https://example.com"), request, user);

        verify(rateLimiterService).tryConsume("192.168.1.5");
    }

    @Test
    void getUrls_returnsTheUsersUrls() {
        List<UrlResponse> urls = List.of(
                new UrlResponse("abc", "https://example.com", LocalDateTime.now()));
        when(urlService.getUrlsByUser(user)).thenReturn(urls);

        ResponseEntity<ApiResponse<List<UrlResponse>>> response = controller.getUrls(user);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(urls, response.getBody().data());
    }

    @Test
    void deleteUrl_delegatesToServiceForTheAuthenticatedUser() {
        ResponseEntity<ApiResponse<Void>> response = controller.deleteUrl("abc", user);

        assertEquals(200, response.getStatusCode().value());
        verify(urlService).deleteUserUrl("abc", user);
    }

    @Test
    void shorten_usesFirstAddressOfForwardedForList() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1, 10.0.0.2");
        when(rateLimiterService.tryConsume("203.0.113.7")).thenReturn(true);
        when(urlService.shortenUrl(any(), any())).thenReturn("abc");

        controller.shortenUrl(body("https://example.com"), request, user);

        verify(rateLimiterService).tryConsume("203.0.113.7");
    }
}
